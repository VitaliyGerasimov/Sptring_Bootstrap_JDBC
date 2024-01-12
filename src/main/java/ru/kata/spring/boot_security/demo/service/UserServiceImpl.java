package ru.kata.spring.boot_security.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.kata.spring.boot_security.demo.model.Role;
import ru.kata.spring.boot_security.demo.model.User;
import ru.kata.spring.boot_security.demo.repository.RoleRepository;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.sql.*;
import java.util.*;


@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final DataSource dataSource;
    private final RoleRepository roleRepository;

    @Autowired
    public UserServiceImpl(DataSource dataSource, RoleRepository roleRepository) {
        this.dataSource = dataSource;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapUser(resultSet);
            } else {
                throw new UsernameNotFoundException("User not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error loading user by username", e);
        }
    }

    @Override
    @Transactional
    public User findByUsername(String username) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapUser(resultSet);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by username", e);
        }
    }

    @Override
    @Transactional
    public void createNewUser(User user, String roleName) {
        String sql = "INSERT INTO user (email, password, name, last_name) VALUES (?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getPassword());
            preparedStatement.setString(3, user.getName());
            preparedStatement.setString(4, user.getLast_name());
            preparedStatement.executeUpdate();


            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long userId = generatedKeys.getLong(1);
                    Role userRole = roleRepository.findById(Long.valueOf(roleName)).orElseThrow();
                    System.out.println("Role found: " + userRole);
                    if (userRole == null) {
                        throw new RuntimeException("Role not found for name: " + roleName);
                    }

                    String sql2 = "INSERT INTO users_roles (user_id, role_id) VALUES (?, ?)";
                    try (PreparedStatement prepareStatement = connection.prepareStatement(sql2)) {
                        prepareStatement.setLong(1, userId);
                        prepareStatement.setLong(2, userRole.getId());
                        prepareStatement.executeUpdate();
                    }
                } else {
                    throw new SQLException("Creating user failed, no ID.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error creating user", e);
        }
    }

    @Override
    public List<User> allUsers() {
        String sql = "SELECT * FROM user";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all users", e);
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        String sql1 = "DELETE FROM user WHERE id = ?";
        String sql2 = "DELETE FROM users_roles WHERE user_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement deleteUserStatement = connection.prepareStatement(sql1);
             PreparedStatement deleteUsersRolesStatement = connection.prepareStatement(sql2)) {
            deleteUsersRolesStatement.setLong(1, id);
            deleteUsersRolesStatement.executeUpdate();
            deleteUserStatement.setLong(1, id);
            deleteUserStatement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting user", e);
        }
    }

    @Override
    @Transactional
    public User getUser(Long id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return mapUser(resultSet);
            } else {
                throw new RuntimeException("User not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching user by ID", e);
        }
    }

    @Override
    @Transactional
    public void update(Long id, User user) {
        String sql = "UPDATE user SET last_name = ?, name = ?, password = ?, email = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getLast_name());
            statement.setString(2, user.getName());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getUsername());
            statement.setLong(5, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating user", e);
        }
    }

    @Override
    public List<Role> getAllRoles() {
        String sql = "SELECT * FROM role";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            List<Role> roles = new ArrayList<>();
            while (resultSet.next()) {
                roles.add(mapRole(resultSet));
            }
            return roles;
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching all roles", e);
        }
    }

    public Role mapRole(ResultSet resultSet) throws SQLException {
        Role role = new Role();
        role.setId(resultSet.getLong("id"));
        role.setName(resultSet.getString("name"));
        return role;
    }

    public User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("email"));
        user.setPassword(resultSet.getString("password"));
        user.setName(resultSet.getString("name"));
        user.setLast_name(resultSet.getString("last_name"));
        Set<Role> roles = getUserRoles(user.getId());
        user.setRoles(roles);

        return user;
    }

    private Set<Role> getUserRoles(Long userId) throws SQLException {
        String sql = "SELECT r.* FROM role r JOIN users_roles ur ON r.id = ur.role_id WHERE ur.user_id = ?";
        Set<Role> roles = new HashSet<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                Role role = mapRole(resultSet);
                roles.add(role);
            }
        }
        return roles;
    }
}