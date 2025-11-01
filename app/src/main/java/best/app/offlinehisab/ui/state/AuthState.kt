package best.app.offlinehisab.ui.state

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class SignedIn(val uid: String?, val email: String?) : AuthState()
    data class Error(val message: String?) : AuthState()
}