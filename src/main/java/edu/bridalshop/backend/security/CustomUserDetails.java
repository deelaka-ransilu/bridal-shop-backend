package edu.bridalshop.backend.security;

import edu.bridalshop.backend.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Integer userId;
    private final String publicId;
    private final String email;
    private final String password;
    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId   = user.getUserId();
        this.publicId = user.getPublicId();
        this.email    = user.getEmail();
        this.password = user.getPasswordHash();
        this.isActive = Boolean.TRUE.equals(user.getIsActive());
        this.authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override public String getUsername()            { return email; }
    @Override public String getPassword()            { return password; }
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return isActive; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()             { return isActive; }
}