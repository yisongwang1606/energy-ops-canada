package ca.yisong.energyops.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import ca.yisong.energyops.model.UserAccount;
import ca.yisong.energyops.repository.UserAccountRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public AppUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = findAccount(username);
        return User.builder()
                .username(account.getUsername())
                .password(account.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
                .disabled(!account.isActive())
                .build();
    }

    public UserAccount findAccount(String username) {
        return userAccountRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}
