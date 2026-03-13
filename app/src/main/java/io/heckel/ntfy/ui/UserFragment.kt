package io.heckel.ntfy.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.heckel.ntfy.R
import io.heckel.ntfy.db.Repository
import io.heckel.ntfy.db.User
import io.heckel.ntfy.util.AfterChangedTextWatcher
import io.heckel.ntfy.util.validUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserFragment : DialogFragment() {
    private var user: User? = null
    private lateinit var baseUrlsInUse: ArrayList<String>
    private lateinit var listener: UserDialogListener
    private lateinit var repository: Repository

    private lateinit var toolbar: MaterialToolbar
    private lateinit var saveMenuItem: MenuItem
    private lateinit var deleteMenuItem: MenuItem
    private lateinit var descriptionView: TextView
    private lateinit var baseUrlViewLayout: TextInputLayout
    private lateinit var baseUrlView: TextInputEditText
    private lateinit var usernameView: TextInputEditText
    private lateinit var passwordView: TextInputEditText

    interface UserDialogListener {
        fun onAddUser(dialog: DialogFragment, user: User)
        fun onUpdateUser(dialog: DialogFragment, user: User)
        fun onDeleteUser(dialog: DialogFragment, baseUrl: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UserDialogListener) {
            listener = context
        }
        repository = Repository.getInstance(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (activity == null) {
            throw IllegalStateException("Activity cannot be null")
        }

        val baseUrl = arguments?.getString(BUNDLE_BASE_URL)
        val username = arguments?.getString(BUNDLE_USERNAME)
        val password = arguments?.getString(BUNDLE_PASSWORD)

        if (baseUrl != null && username != null && password != null) {
            user = User(baseUrl, username, password)
        }

        baseUrlsInUse = arguments?.getStringArrayList(BUNDLE_BASE_URLS_IN_USE) ?: arrayListOf()

        val view = requireActivity().layoutInflater.inflate(R.layout.fragment_user_dialog, null)

        this.isCancelable = true
        toolbar = view.findViewById(R.id.user_dialog_toolbar)
        toolbar.navigationIcon = null

        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp) // Asegúrate que este drawable existe o usa ic_cancel
        toolbar.setNavigationOnClickListener {
            dismiss() // Cierra el fragmento al tocar la X
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.user_dialog_action_save -> {
                    saveClicked()
                    true
                }
                R.id.user_dialog_action_delete -> {
                    // 1. Limpiar las credenciales guardadas
                    val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply() // Borra global_username y global_password

                    // 2. Notificar al listener (MainActivity) para que borre de la DB
                    if (this::listener.isInitialized) {
                        listener.onDeleteUser(this, user!!.baseUrl)
                    }

                    // 3. Cerrar el diálogo y avisar que debe mostrarse de nuevo para un nuevo login
                    dismiss()

                    // Opcional: Si quieres que aparezca el login vacío inmediatamente:
                    val newLogin = UserFragment()
                    newLogin.show(parentFragmentManager, "UserFragment")

                    true
                }

                else -> false
            }
        }
        saveMenuItem = toolbar.menu.findItem(R.id.user_dialog_action_save)
        deleteMenuItem = toolbar.menu.findItem(R.id.user_dialog_action_delete)

        descriptionView = view.findViewById(R.id.user_dialog_description)
        baseUrlViewLayout = view.findViewById(R.id.user_dialog_base_url_layout)
        baseUrlView = view.findViewById(R.id.user_dialog_base_url)
        usernameView = view.findViewById(R.id.user_dialog_username)
        passwordView = view.findViewById(R.id.user_dialog_password)

        if (user == null) {
            toolbar.setTitle(R.string.user_dialog_title_add)
            descriptionView.text = getString(R.string.user_dialog_description_add)

            // Aseguramos que el layout de la URL esté oculto para el cliente final
            baseUrlViewLayout.visibility = View.GONE

            passwordView.hint = getString(R.string.user_dialog_password_hint_add)
            saveMenuItem.setTitle(R.string.user_dialog_button_add)
            deleteMenuItem.isVisible = false
        } else {
            toolbar.setTitle(R.string.user_dialog_title_edit)
            descriptionView.text = getString(R.string.user_dialog_description_edit)
            baseUrlViewLayout.visibility = View.GONE
            usernameView.setText(user!!.username)
            passwordView.hint = getString(R.string.user_dialog_password_hint_edit)
            saveMenuItem.setTitle(R.string.common_button_save)
            deleteMenuItem.isVisible = true
        }

        val textWatcher = AfterChangedTextWatcher {
            validateInput()
        }
        baseUrlView.addTextChangedListener(textWatcher)
        usernameView.addTextChangedListener(textWatcher)
        passwordView.addTextChangedListener(textWatcher)

        val dialog = Dialog(requireContext(), R.style.Theme_App_FullScreenDialog)
        dialog.setContentView(view)

        validateInput()

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onResume() {
        super.onResume()
        val focusView = usernameView
        focusView.postDelayed({
            focusView.requestFocus()
            if (user != null && usernameView.text != null) {
                usernameView.setSelection(usernameView.text!!.length)
            }
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(focusView, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
    }
    private fun saveClicked() {
        if (!this::listener.isInitialized) return
        val baseUrl = baseUrlView.text?.toString() ?: ""
        val username = usernameView.text?.toString() ?: ""
        val password = passwordView.text?.toString() ?: ""
        if (user == null) {
            user = User(baseUrl, username, password)
            listener.onAddUser(this, user!!)
        } else {
            user = if (password.isNotEmpty()) {
                user!!.copy(username = username, password = password)
            } else {
                user!!.copy(username = username)
            }
            listener.onUpdateUser(this, user!!)
        }
        dismiss()
    }

    private fun validateInput() {
        if (!this::saveMenuItem.isInitialized) return

        val username = usernameView.text?.toString() ?: ""
        val password = passwordView.text?.toString() ?: ""

        baseUrlViewLayout.error = null

        // Habilitar el botón solo si usuario y contraseña tienen texto
        saveMenuItem.isEnabled = username.isNotEmpty() && password.isNotEmpty()
    }

    /* private fun saveClicked() {
        // Obtenemos la URL de forma segura sin depender del campo de texto
        val baseUrl = repository.getDefaultBaseUrl() ?: getString(R.string.app_base_url)
        val username = usernameView.text?.toString() ?: ""
        val password = passwordView.text?.toString() ?: ""

        if (username.isNotEmpty() && password.isNotEmpty()) {
            val newUser = User(baseUrl, username, password)

            CoroutineScope(Dispatchers.IO).launch {
                repository.addUser(newUser)
            }

            if (this::listener.isInitialized) {
                if (user == null) {
                    listener.onAddUser(this, newUser)
                } else {
                    listener.onUpdateUser(this, newUser)
                }
            }
            dismiss()
        }
    }

    private fun validateInput() {
        if (!this::saveMenuItem.isInitialized) return

        // Aquí resolvemos el problema: Inyectamos la URL directamente en lugar de buscarla en la vista
        val baseUrl = user?.baseUrl ?: (repository.getDefaultBaseUrl() ?: getString(R.string.app_base_url))

        val username = usernameView.text?.toString() ?: ""
        val password = passwordView.text?.toString() ?: ""

        baseUrlViewLayout.error = null

        if (user == null) {
            CoroutineScope(Dispatchers.Main).launch {
                val hasAuthorizationHeader = hasAuthorizationHeader(baseUrl)
                if (hasAuthorizationHeader) {
                    baseUrlViewLayout.error = getString(R.string.user_dialog_base_url_error_authorization_header_exists)
                }
                // Al evaluar una URL válida y no vacía, el botón por fin se habilitará
                saveMenuItem.isEnabled = validUrl(baseUrl)
                        && !baseUrlsInUse.contains(baseUrl)
                        && !hasAuthorizationHeader
                        && username.isNotEmpty()
                        && password.isNotEmpty()
            }
        } else {
            saveMenuItem.isEnabled = username.isNotEmpty()
        }
    } */

    private suspend fun hasAuthorizationHeader(baseUrl: String): Boolean {
        if (!this::repository.isInitialized || !validUrl(baseUrl)) {
            return false
        }
        return withContext(Dispatchers.IO) {
            repository.getCustomHeaders(baseUrl)
                .any { it.name.equals("Authorization", ignoreCase = true) }
        }
    }

    companion object {
        const val TAG = "NtfyUserFragment"
        private const val BUNDLE_BASE_URL = "baseUrl"
        private const val BUNDLE_USERNAME = "username"
        private const val BUNDLE_PASSWORD = "password"
        private const val BUNDLE_BASE_URLS_IN_USE = "baseUrlsInUse"

        fun newInstance(user: User?, baseUrlsInUse: ArrayList<String>): UserFragment {
            val fragment = UserFragment()
            val args = Bundle()
            args.putStringArrayList(BUNDLE_BASE_URLS_IN_USE, baseUrlsInUse)
            if (user != null) {
                args.putString(BUNDLE_BASE_URL, user.baseUrl)
                args.putString(BUNDLE_USERNAME, user.username)
                args.putString(BUNDLE_PASSWORD, user.password)
            }
            fragment.arguments = args
            return fragment
        }
    }
}