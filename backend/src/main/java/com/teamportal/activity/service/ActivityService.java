package com.teamportal.activity.service;

import com.teamportal.activity.dto.ActivityFilter;
import com.teamportal.activity.dto.ActivityResponse;
import com.teamportal.activity.dto.PageResponse;
import com.teamportal.activity.model.ActivityAction;
import com.teamportal.activity.model.ActivityLog;
import com.teamportal.activity.repository.ActivityLogRepository;
import com.teamportal.board.model.Board;
import com.teamportal.board.model.BoardStatus;
import com.teamportal.note.model.Note;
import com.teamportal.note.model.NoteStatus;
import com.teamportal.security.AuthenticatedUser;
import com.teamportal.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registra y consulta el historial de actividad (quién hizo qué y cuándo).
 * Los métodos de registro se llaman dentro de la misma transacción que el cambio,
 * así una acción que falla no deja rastro en el historial.
 */
@Service
@Transactional
public class ActivityService {

    private static final int MAX_PAGE_SIZE = 100;

    @PersistenceContext
    private EntityManager entityManager;

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityService(ActivityLogRepository activityLogRepository, UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ registro

    public void noteCreated(Note note, AuthenticatedUser actor) {
        ActivityLog log = forNote(ActivityAction.NOTE_CREATED, note, actor);
        log.setToValue(note.getStatus().name());
        activityLogRepository.save(log);
    }

    public void noteUpdated(Note note, AuthenticatedUser actor) {
        activityLogRepository.save(forNote(ActivityAction.NOTE_UPDATED, note, actor));
    }

    public void noteStatusChanged(Note note, NoteStatus previousStatus, AuthenticatedUser actor) {
        ActivityLog log = forNote(ActivityAction.NOTE_STATUS_CHANGED, note, actor);
        log.setFromValue(previousStatus.name());
        log.setToValue(note.getStatus().name());
        activityLogRepository.save(log);
    }

    public void noteMoved(Note note, AuthenticatedUser actor) {
        activityLogRepository.save(forNote(ActivityAction.NOTE_MOVED, note, actor));
    }

    public void noteDeleted(Note note, AuthenticatedUser actor) {
        ActivityLog log = forNote(ActivityAction.NOTE_DELETED, note, actor);
        log.setFromValue(note.getStatus().name());
        activityLogRepository.save(log);
    }

    public void boardCreated(Board board, AuthenticatedUser actor) {
        ActivityLog log = forBoard(ActivityAction.BOARD_CREATED, board, actor);
        log.setToValue(board.getStatus().name());
        activityLogRepository.save(log);
    }

    public void boardUpdated(Board board, AuthenticatedUser actor) {
        activityLogRepository.save(forBoard(ActivityAction.BOARD_UPDATED, board, actor));
    }

    public void boardStatusChanged(Board board, BoardStatus previousStatus, AuthenticatedUser actor) {
        ActivityLog log = forBoard(ActivityAction.BOARD_STATUS_CHANGED, board, actor);
        log.setFromValue(previousStatus.name());
        log.setToValue(board.getStatus().name());
        activityLogRepository.save(log);
    }

    public void boardDeleted(Board board, long deletedNotes, AuthenticatedUser actor) {
        ActivityLog log = forBoard(ActivityAction.BOARD_DELETED, board, actor);
        log.setDetails(deletedNotes == 1 ? "Se eliminó 1 nota" : "Se eliminaron " + deletedNotes + " notas");
        activityLogRepository.save(log);
    }

    private ActivityLog forNote(ActivityAction action, Note note, AuthenticatedUser actor) {
        ActivityLog log = base(action, actor);
        log.setBoardId(note.getBoard().getId());
        log.setBoardName(note.getBoard().getName());
        log.setNoteId(note.getId());
        log.setNoteTitle(truncate(note.getTitle(), 255));
        log.setNoteOwner(note.getCreatedBy());
        return log;
    }

    private ActivityLog forBoard(ActivityAction action, Board board, AuthenticatedUser actor) {
        ActivityLog log = base(action, actor);
        log.setBoardId(board.getId());
        log.setBoardName(truncate(board.getName(), 100));
        return log;
    }

    private ActivityLog base(ActivityAction action, AuthenticatedUser actor) {
        ActivityLog log = new ActivityLog();
        log.setAction(action);
        log.setUser(userRepository.getReferenceById(actor.getId()));
        return log;
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    // ------------------------------------------------------------------ consultas

    /** Historial paginado, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> search(ActivityFilter filter, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Where where = Where.from(filter);

        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(a) from ActivityLog a" + where.clause(), Long.class);
        where.bind(countQuery);
        long total = countQuery.getSingleResult();

        TypedQuery<ActivityLog> listQuery = entityManager.createQuery(
                "select a from ActivityLog a join fetch a.user left join fetch a.noteOwner"
                        + where.clause() + " order by a.createdAt desc, a.id desc", ActivityLog.class);
        where.bind(listQuery);
        listQuery.setFirstResult(safePage * safeSize);
        listQuery.setMaxResults(safeSize);

        List<ActivityResponse> content = listQuery.getResultList().stream()
                .map(ActivityResponse::from)
                .toList();
        return PageResponse.of(content, safePage, safeSize, total);
    }

    /** Cantidad de acciones por tipo. */
    @Transactional(readOnly = true)
    public Map<ActivityAction, Long> countByAction(ActivityFilter filter) {
        Where where = Where.from(filter);
        TypedQuery<Object[]> query = entityManager.createQuery(
                "select a.action, count(a) from ActivityLog a" + where.clause() + " group by a.action", Object[].class);
        where.bind(query);

        Map<ActivityAction, Long> result = new EnumMap<>(ActivityAction.class);
        for (Object[] row : query.getResultList()) {
            result.put((ActivityAction) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    /** Por usuario: cantidad de acciones y fecha de la última. */
    @Transactional(readOnly = true)
    public Map<Long, UserActivityStats> statsByUser(ActivityFilter filter) {
        Where where = Where.from(filter);
        TypedQuery<Object[]> query = entityManager.createQuery(
                "select a.user.id, count(a), max(a.createdAt) from ActivityLog a" + where.clause() + " group by a.user.id",
                Object[].class);
        where.bind(query);

        Map<Long, UserActivityStats> result = new HashMap<>();
        for (Object[] row : query.getResultList()) {
            Long userId = ((Number) row[0]).longValue();
            result.put(userId, new UserActivityStats(((Number) row[1]).longValue(), (LocalDateTime) row[2]));
        }
        return result;
    }

    public record UserActivityStats(long count, LocalDateTime lastActivityAt) {
    }

    /** Arma el WHERE solo con los filtros presentes. */
    private static final class Where {
        private final List<String> conditions = new ArrayList<>();
        private final Map<String, Object> params = new LinkedHashMap<>();

        static Where from(ActivityFilter filter) {
            Where where = new Where();
            if (filter == null) {
                return where;
            }
            if (filter.userId() != null) {
                where.add("a.user.id = :userId", "userId", filter.userId());
            }
            if (filter.boardId() != null) {
                where.add("a.boardId = :boardId", "boardId", filter.boardId());
            }
            if (filter.action() != null) {
                where.add("a.action = :action", "action", filter.action());
            }
            if (filter.from() != null) {
                where.add("a.createdAt >= :fromDate", "fromDate", filter.from().atStartOfDay());
            }
            if (filter.to() != null) {
                where.add("a.createdAt < :toDate", "toDate", filter.to().plusDays(1).atStartOfDay());
            }
            return where;
        }

        private void add(String condition, String name, Object value) {
            conditions.add(condition);
            params.put(name, value);
        }

        String clause() {
            return conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
        }

        void bind(TypedQuery<?> query) {
            params.forEach(query::setParameter);
        }
    }
}
