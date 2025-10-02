package ch.obermuhlner.aitutor.auth.exception

class UserNotFoundException(message: String) : RuntimeException(message)

class DuplicateUsernameException(message: String) : RuntimeException(message)

class DuplicateEmailException(message: String) : RuntimeException(message)

class InvalidCredentialsException(message: String = "Invalid username or password") : RuntimeException(message)

class InvalidTokenException(message: String = "Invalid or malformed token") : RuntimeException(message)

class ExpiredTokenException(message: String = "Token has expired") : RuntimeException(message)

class InsufficientPermissionsException(message: String = "Insufficient permissions to perform this action") : RuntimeException(message)

class WeakPasswordException(message: String) : RuntimeException(message)

class AccountDisabledException(message: String = "Account is disabled") : RuntimeException(message)

class AccountLockedException(message: String = "Account is locked") : RuntimeException(message)
