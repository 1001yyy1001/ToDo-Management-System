package com.dmm.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {

    @Autowired
    private UsersRepository repository;

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        if (userName == null || "".equals(userName)) {
            throw new UsernameNotFoundException("ユーザー名が空です");
        }

        Users user = repository.findById(userName).orElseThrow(() -> 
            new UsernameNotFoundException(userName + "は見つかりません。")
        );
        return new AccountUserDetails(user);
    }
}
