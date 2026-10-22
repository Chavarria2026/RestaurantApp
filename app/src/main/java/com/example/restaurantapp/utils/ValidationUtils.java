package com.example.restaurantapp.utils;

import android.util.Patterns;

public class ValidationUtils {

    // Validar email
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Validar usuario (mínimo 3 caracteres)
    public static boolean isValidUsername(String username) {
        return username != null && username.length() >= 3;
    }

    // Validar nombre completo (mínimo 7 caracteres según el PDF)
    public static boolean isValidFullName(String name) {
        return name != null && name.length() >= 7;
    }

    // Validar contraseña (mínimo 8 caracteres, letras y números según el PDF)
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*");
    }

    // Validar que dos contraseñas coincidan
    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

    // Validar campo no vacío
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    // Validar número positivo
    public static boolean isPositiveNumber(double number) {
        return number > 0;
    }

    // Validar cantidad (mayor a 0)
    public static boolean isValidQuantity(int quantity) {
        return quantity > 0;
    }

    // Validar precio (mayor a 0)
    public static boolean isValidPrice(double price) {
        return price > 0;
    }

    // Validar teléfono (mínimo 8 dígitos)
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return phone.matches("\\d{8,}");
    }

    // Validar que sea solo números
    public static boolean isOnlyNumbers(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("\\d+");
    }
}