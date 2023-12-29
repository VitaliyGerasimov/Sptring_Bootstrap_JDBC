package ru.kata.spring.boot_security.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.service.UserServiceImpl;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserServiceImpl userService;

    @Autowired
    public AdminController(UserServiceImpl userService) {
        this.userService = userService;
    }

    @GetMapping
    public String showAllUsers(Principal principal, Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("users", userService.allUsers());
        model.addAttribute("admin", userService.findByUsername(principal.getName()));
        model.addAttribute("newUser", new User());
        model.addAttribute("rolesAdd", userService.getAllRoles());
        return "admin";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") long id) {
        userService.deleteUser(id);
        return "redirect:/admin";
    }

    @PostMapping("/{id}")
    public String update(@ModelAttribute("user") User user, @PathVariable("id") long id) {
        userService.update(id, user);
        return "redirect:/admin";
    }

    @PostMapping()
    public String addCreateNewUser(@ModelAttribute("user") User user) {
        userService.createNewUser(user);
        return "redirect:/admin";
    }
}
