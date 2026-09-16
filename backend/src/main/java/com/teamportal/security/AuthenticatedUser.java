package com.teamportal.security;

import com.teamportal.user.model.Role;
import com.teamportal.user.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Usuario autenticado de la petición actual. Se obtiene en los controladores con
 * {@code @AuthenticationPrincipal AuthenticatedUser currentUser}.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String name;
    private final String email;
    private final String password;
    private final Role role;
    private final boolean active;

    private AuthenticatedUser(Long id, String name, String email, String password, Role role, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
    }

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(
                user.getId(), user.getName(), user.getEmail(), user.getPassword(), user.getRole(), user.isActive());
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public Role getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return active; }
}
