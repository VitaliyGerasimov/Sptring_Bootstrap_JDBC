package ru.kata.spring.boot_security.demo.service;

import org.springframework.validation.BindingResult;
import ru.kata.spring.boot_security.demo.model.User;

import java.util.List;

public interface UserService {
    public void validate(User user, BindingResult bindingResult);
    public User findByUsername(String username);
    public void register(User user);
    public List<User> allUsers();
    public User getUserById(Long id);
    public void deleteUser(Long id);
    public List<User> usergtList(Long idMin);
    public void update(Long id, User user);
}
