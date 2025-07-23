package movieMentor.security;

import lombok.RequiredArgsConstructor;
import movieMentor.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user either by username or email address.
     *
     * @param input username or email
     * @return UserDetails instance for authentication
     * @throws UsernameNotFoundException if no user is found
     */
    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(input, input)
                .map(UserDetailsImpl::new)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with username or email: " + input)
                );
    }
}
