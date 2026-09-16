package com.teamportal.dashboard.service;

import com.teamportal.activity.dto.ActivityFilter;
import com.teamportal.activity.model.ActivityAction;
import com.teamportal.activity.service.ActivityService;
import com.teamportal.activity.service.ActivityService.UserActivityStats;
import com.teamportal.board.model.Board;
import com.teamportal.board.model.BoardStatus;
import com.teamportal.board.model.BoardType;
import com.teamportal.board.repository.BoardRepository;
import com.teamportal.dashboard.dto.DashboardFilter;
import com.teamportal.dashboard.dto.DashboardResponse;
import com.teamportal.dashboard.dto.DashboardResponse.BoardProgress;
import com.teamportal.dashboard.dto.DashboardResponse.CountItem;
import com.teamportal.dashboard.dto.DashboardResponse.NoteRow;
import com.teamportal.dashboard.dto.DashboardResponse.StatusCounts;
import com.teamportal.dashboard.dto.DashboardResponse.Timeline;
import com.teamportal.dashboard.dto.DashboardResponse.TimelinePoint;
import com.teamportal.dashboard.dto.DashboardResponse.Totals;
import com.teamportal.dashboard.dto.DashboardResponse.UserHighlight;
import com.teamportal.dashboard.dto.DashboardResponse.UserProgress;
import com.teamportal.note.model.Note;
import com.teamportal.note.model.NoteStatus;
import com.teamportal.note.repository.NoteRepository;
import com.teamportal.user.dto.UserSummary;
import com.teamportal.user.model.User;
import com.teamportal.user.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula las métricas del dashboard. Todos los roles ven la misma información.
 * Las notas se filtran en memoria (volumen de un equipo) para que todas las gráficas
 * usen exactamente el mismo conjunto de datos.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** Rango por defecto de la línea de tiempo cuando no hay fechas en el filtro. */
    private static final int DEFAULT_TIMELINE_DAYS = 30;
    /** Hasta este número de días la línea de tiempo va por día; después, por semana. */
    private static final int MAX_DAILY_POINTS = 92;

    private final NoteRepository noteRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;

    public DashboardService(NoteRepository noteRepository, BoardRepository boardRepository,
                            UserRepository userRepository, ActivityService activityService) {
        this.noteRepository = noteRepository;
        this.boardRepository = boardRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
    }

    public DashboardResponse getDashboard(DashboardFilter rawFilter) {
        DashboardFilter filter = normalize(rawFilter);

        // ---- Tableros dentro del filtro
        List<Board> boards = boardRepository.findAllByOrderByNameAsc().stream()
                .filter(board -> filter.boardId() == null || filter.boardId().equals(board.getId()))
                .filter(board -> filter.boardType() == null || filter.boardType() == board.getType())
                .filter(board -> filter.boardStatus() == null || filter.boardStatus() == board.getStatus())
                .toList();
        Set<Long> boardIds = boards.stream().map(Board::getId).collect(Collectors.toSet());

        // ---- Notas de esos tableros (sin filtro de fecha: se usa para la línea de tiempo)
        List<Note> notesInBoards = noteRepository.findAllWithBoardAndCreator().stream()
                .filter(note -> boardIds.contains(note.getBoard().getId()))
                .filter(note -> filter.userId() == null || note.isOwnedBy(filter.userId()))
                .toList();

        // ---- Notas dentro de todo el filtro (fecha de creación incluida)
        List<Note> notes = notesInBoards.stream()
                .filter(note -> isWithin(note.getCreatedAt(), filter.from(), filter.to()))
                .toList();

        // ---- Historial
        ActivityFilter activityFilter = new ActivityFilter(filter.userId(), filter.boardId(), null, filter.from(), filter.to());
        Map<ActivityAction, Long> actionCounts = activityService.countByAction(activityFilter);
        Map<Long, UserActivityStats> activityByUser = activityService.statsByUser(activityFilter);

        StatusCounts overall = countStatuses(notes);
        long contributors = notes.stream()
                .map(Note::getCreatedBy)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .count();
        long totalActivities = actionCounts.values().stream().mapToLong(Long::longValue).sum();

        Totals totals = new Totals(
                boards.size(),
                overall.total(),
                overall.pending(),
                overall.inProgress(),
                overall.completed(),
                overall.completionRate(),
                contributors,
                totalActivities
        );

        List<UserProgress> users = buildUserProgress(filter, notes, activityByUser);

        return new DashboardResponse(
                LocalDateTime.now(),
                totals,
                List.of(
                        new CountItem(NoteStatus.PENDING.name(), overall.pending()),
                        new CountItem(NoteStatus.IN_PROGRESS.name(), overall.inProgress()),
                        new CountItem(NoteStatus.COMPLETED.name(), overall.completed())
                ),
                countByEnum(BoardType.values(), boards.stream().map(Board::getType).toList()),
                countByEnum(BoardStatus.values(), boards.stream().map(Board::getStatus).toList()),
                buildBoardProgress(boards, notes),
                users,
                mostNotes(users),
                fewestNotes(users),
                Arrays.stream(ActivityAction.values())
                        .map(action -> new CountItem(action.name(), actionCounts.getOrDefault(action, 0L)))
                        .toList(),
                buildTimeline(filter, notesInBoards),
                buildNoteRows(notes)
        );
    }

    // ------------------------------------------------------------------ bloques

    private List<BoardProgress> buildBoardProgress(List<Board> boards, List<Note> notes) {
        Map<Long, List<Note>> notesByBoard = notes.stream()
                .collect(Collectors.groupingBy(note -> note.getBoard().getId()));

        return boards.stream()
                .map(board -> {
                    List<Note> boardNotes = notesByBoard.getOrDefault(board.getId(), List.of());
                    LocalDateTime lastActivity = boardNotes.stream()
                            .map(DashboardService::lastChange)
                            .filter(Objects::nonNull)
                            .max(Comparator.naturalOrder())
                            .orElse(null);
                    return new BoardProgress(board.getId(), board.getName(), board.getType(), board.getStatus(),
                            countStatuses(boardNotes), lastActivity);
                })
                .sorted(Comparator.comparingLong((BoardProgress b) -> b.notes().total()).reversed()
                        .thenComparing(BoardProgress::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Un registro por empleado, ordenado de más a menos notas. Incluye a los usuarios
     * activos sin notas (para ver quién no ha generado ninguna) y a los inactivos que sí tienen.
     */
    private List<UserProgress> buildUserProgress(DashboardFilter filter, List<Note> notes,
                                                 Map<Long, UserActivityStats> activityByUser) {
        Map<Long, List<Note>> notesByUser = notes.stream()
                .filter(note -> note.getCreatedBy() != null)
                .collect(Collectors.groupingBy(note -> note.getCreatedBy().getId()));

        return userRepository.findAll(Sort.by("name")).stream()
                .filter(user -> filter.userId() == null || filter.userId().equals(user.getId()))
                .filter(user -> user.isActive()
                        || notesByUser.containsKey(user.getId())
                        || activityByUser.containsKey(user.getId()))
                .map(user -> {
                    UserActivityStats stats = activityByUser.get(user.getId());
                    return new UserProgress(
                            user.getId(),
                            user.getName(),
                            user.getRole(),
                            user.isActive(),
                            countStatuses(notesByUser.getOrDefault(user.getId(), List.of())),
                            stats != null ? stats.count() : 0L,
                            stats != null ? stats.lastActivityAt() : null
                    );
                })
                .sorted(Comparator.comparingLong((UserProgress u) -> u.notes().total()).reversed()
                        .thenComparing(UserProgress::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private UserHighlight mostNotes(List<UserProgress> users) {
        return users.stream()
                .filter(user -> user.notes().total() > 0)
                .findFirst()
                .map(user -> new UserHighlight(user.id(), user.name(), user.notes().total()))
                .orElse(null);
    }

    /** El empleado activo con menos notas (puede tener cero). */
    private UserHighlight fewestNotes(List<UserProgress> users) {
        return users.stream()
                .filter(UserProgress::active)
                .min(Comparator.comparingLong((UserProgress u) -> u.notes().total())
                        .thenComparing(UserProgress::name, String.CASE_INSENSITIVE_ORDER))
                .map(user -> new UserHighlight(user.id(), user.name(), user.notes().total()))
                .orElse(null);
    }

    /** Notas creadas y completadas por día (o por semana si el rango es largo). */
    private Timeline buildTimeline(DashboardFilter filter, List<Note> notes) {
        LocalDate today = LocalDate.now();
        LocalDate to = filter.to() != null ? filter.to() : today;
        LocalDate from = filter.from() != null ? filter.from() : to.minusDays(DEFAULT_TIMELINE_DAYS - 1L);

        boolean weekly = ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAILY_POINTS;
        LocalDate start = weekly ? from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : from;

        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (LocalDate date = start; !date.isAfter(to); date = weekly ? date.plusWeeks(1) : date.plusDays(1)) {
            buckets.put(date, new long[2]);
        }

        for (Note note : notes) {
            addToBucket(buckets, note.getCreatedAt(), from, to, weekly, 0);
            addToBucket(buckets, note.getCompletedAt(), from, to, weekly, 1);
        }

        List<TimelinePoint> points = buckets.entrySet().stream()
                .map(entry -> new TimelinePoint(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
        return new Timeline(from, to, weekly ? "WEEK" : "DAY", points);
    }

    private void addToBucket(Map<LocalDate, long[]> buckets, LocalDateTime moment, LocalDate from, LocalDate to,
                             boolean weekly, int index) {
        if (moment == null) {
            return;
        }
        LocalDate date = moment.toLocalDate();
        if (date.isBefore(from) || date.isAfter(to)) {
            return;
        }
        LocalDate key = weekly ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : date;
        long[] bucket = buckets.get(key);
        if (bucket != null) {
            bucket[index]++;
        }
    }

    private List<NoteRow> buildNoteRows(List<Note> notes) {
        return notes.stream()
                .sorted(Comparator.comparing(DashboardService::lastChange, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(note -> new NoteRow(
                        note.getId(),
                        note.getTitle(),
                        note.getStatus(),
                        note.getBoard().getId(),
                        note.getBoard().getName(),
                        note.getBoard().getType(),
                        note.getBoard().getStatus(),
                        UserSummary.from(note.getCreatedBy()),
                        note.getCreatedAt(),
                        note.getUpdatedAt(),
                        note.getCompletedAt()
                ))
                .toList();
    }

    // ------------------------------------------------------------------ utilidades

    private static StatusCounts countStatuses(Collection<Note> notes) {
        long pending = 0;
        long inProgress = 0;
        long completed = 0;
        for (Note note : notes) {
            switch (note.getStatus()) {
                case PENDING -> pending++;
                case IN_PROGRESS -> inProgress++;
                case COMPLETED -> completed++;
            }
        }
        long total = pending + inProgress + completed;
        return new StatusCounts(pending, inProgress, completed, total, percentage(completed, total));
    }

    private static <E extends Enum<E>> List<CountItem> countByEnum(E[] values, List<E> items) {
        List<CountItem> result = new ArrayList<>();
        for (E value : values) {
            long count = items.stream().filter(item -> item == value).count();
            result.add(new CountItem(value.name(), count));
        }
        return result;
    }

    private static double percentage(long part, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round(part * 1000.0 / total) / 10.0;
    }

    private static LocalDateTime lastChange(Note note) {
        LocalDateTime last = note.getCreatedAt();
        if (note.getUpdatedAt() != null && (last == null || note.getUpdatedAt().isAfter(last))) {
            last = note.getUpdatedAt();
        }
        return last;
    }

    private static boolean isWithin(LocalDateTime moment, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return true;
        }
        if (moment == null) {
            return false;
        }
        LocalDate date = moment.toLocalDate();
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    /** Si las fechas vienen invertidas se intercambian. */
    private static DashboardFilter normalize(DashboardFilter filter) {
        if (filter == null) {
            return new DashboardFilter(null, null, null, null, null, null);
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            return new DashboardFilter(filter.boardId(), filter.boardType(), filter.boardStatus(),
                    filter.userId(), filter.to(), filter.from());
        }
        return filter;
    }
}
