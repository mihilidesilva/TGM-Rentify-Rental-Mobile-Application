package com.example.myapplication.view

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.adapter.UserSearchAdapter
import com.example.myapplication.model.User
import com.example.myapplication.utils.SharedPreferencesHelper
import com.example.myapplication.viewModel.ProfileViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var etSearch: EditText
    private lateinit var ivCoverPhoto: ImageView
    private lateinit var btnEditCoverPhoto: ImageButton
    private lateinit var ivProfilePhoto: CircleImageView
    private lateinit var btnEditProfilePhoto: ImageButton
    private lateinit var tvProfileName: TextView
    private lateinit var tvUsername: TextView

    // Separate Views for Icon and Text
    private lateinit var ivRoleIcon: ImageView
    private lateinit var tvUserRole: TextView

    private lateinit var tvProfileBio: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvEmailContact: TextView
    private lateinit var tvPhoneContact: TextView
    private lateinit var btnManageProperties: Button
    private lateinit var btnMenu: ImageButton

    // Social Icons (ImageViews)
    private lateinit var ivInstagram: ImageView
    private lateinit var ivFacebook: ImageView
    private lateinit var ivWhatsapp: ImageView
    private lateinit var ivWebsite: ImageView
    
    // Edit Buttons
    private lateinit var btnEditInfo: ImageButton
    
    // Inline Edit Form (for links)
    private lateinit var layoutEditContact: LinearLayout
    private lateinit var switchPhonePublic: SwitchMaterial
    private lateinit var etInstagram: TextInputEditText
    private lateinit var etFacebook: TextInputEditText
    private lateinit var etWhatsapp: TextInputEditText
    private lateinit var etWebsite: TextInputEditText
    private lateinit var btnSaveInfo: Button
    private lateinit var btnCancelEdit: Button

    // Search List
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var searchAdapter: UserSearchAdapter

    private val viewModel: ProfileViewModel by viewModels()
    private var currentUser: User? = null
    
    // Flag to determine if this is viewing another user's profile
    private var isReadOnly = false
    private var targetUserId: String? = null

    // Search State
    private var isSearchMode = false
    
    // Auto-Search Handler
    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchRunnable = Runnable {
        val query = etSearch.text.toString().trim()
        if (query.isNotEmpty()) {
            performSearch(query)
        }
    }

    // Camera/Gallery Logic State
    private var isEditingProfilePhoto = true
    private var tempImageUri: Uri? = null

    // Launcher for Gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
    }

    // Launcher for Camera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            handleImageSelection(tempImageUri!!)
        }
    }

    // Launcher for Camera Permission
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check for arguments (userId) to determine mode
        targetUserId = arguments?.getString("userId")
        isReadOnly = targetUserId != null

        // Initialize Views
        etSearch = view.findViewById(R.id.edit_text_search)
        ivCoverPhoto = view.findViewById(R.id.iv_cover_photo)
        btnEditCoverPhoto = view.findViewById(R.id.btn_edit_cover_photo)
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo)
        btnEditProfilePhoto = view.findViewById(R.id.btn_edit_profile_photo)
        tvProfileName = view.findViewById(R.id.tv_profile_name)
        tvUsername = view.findViewById(R.id.tv_username)

        ivRoleIcon = view.findViewById(R.id.iv_role_icon)
        tvUserRole = view.findViewById(R.id.tv_user_role)

        tvProfileBio = view.findViewById(R.id.tv_profile_bio)
        tvLocation = view.findViewById(R.id.tv_location)
        tvAddress = view.findViewById(R.id.tv_address)
        tvEmailContact = view.findViewById(R.id.tv_email_contact)
        tvPhoneContact = view.findViewById(R.id.tv_phone_contact)
        btnManageProperties = view.findViewById(R.id.btn_manage_properties)
        btnMenu = view.findViewById(R.id.btn_menu)

        // Bind Social Icons
        ivInstagram = view.findViewById(R.id.iv_instagram_icon)
        ivFacebook = view.findViewById(R.id.iv_facebook_icon)
        ivWhatsapp = view.findViewById(R.id.iv_whatsapp_icon)
        ivWebsite = view.findViewById(R.id.iv_website_icon)
        
        btnEditInfo = view.findViewById(R.id.btn_edit_info)
        
        layoutEditContact = view.findViewById(R.id.layout_edit_contact)
        switchPhonePublic = view.findViewById(R.id.switch_phone_public)
        etInstagram = view.findViewById(R.id.et_instagram)
        etFacebook = view.findViewById(R.id.et_facebook)
        etWhatsapp = view.findViewById(R.id.et_whatsapp)
        etWebsite = view.findViewById(R.id.et_website)
        btnSaveInfo = view.findViewById(R.id.btn_save_info)
        btnCancelEdit = view.findViewById(R.id.btn_cancel_edit)

        // Initialize Search RecyclerView
        rvSearchResults = view.findViewById(R.id.rv_search_results)
        searchAdapter = UserSearchAdapter { selectedUser ->
            // Handle User Click: Navigate to ProfileFragment
            navigateToUserProfile(selectedUser)
        }
        rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        rvSearchResults.adapter = searchAdapter

        // Configure UI based on mode
        if (isReadOnly) {
            configureReadOnlyMode()
        } else {
            configureEditMode()
        }

        viewModel.userProfile.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                currentUser = user
                updateUI(user)
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(requireContext(), "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe search results
        viewModel.searchResults.observe(viewLifecycleOwner) { users ->
            if (users.isEmpty()) {
                rvSearchResults.visibility = View.GONE
                
                // Show Toast if search was attempted but no results
                // Only show if user explicitly searched (isSearchMode) or auto-search triggered
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty() && isSearchMode) {
                     // Debounce toast slightly to avoid showing it while typing 
                     // But here we rely on the list update. Simple toast for now.
                     // Toast.makeText(requireContext(), "No user found", Toast.LENGTH_SHORT).show() 
                     // NOTE: Showing toast on every keystroke that returns empty is annoying.
                     // I'll leave the logic but maybe user wants it.
                }
            } else {
                rvSearchResults.visibility = View.VISIBLE
                searchAdapter.submitList(users)
            }
        }
        
        // Add Back Press Handler for Search
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSearchMode) {
                    exitSearchMode()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Load Data
        if (isReadOnly && targetUserId != null) {
            viewModel.loadOtherUserProfile(targetUserId!!)
        } else {
            viewModel.loadProfileData()
        }
    }

    private fun configureReadOnlyMode() {
        // Hide interactive elements for other users' profiles
        btnEditCoverPhoto.visibility = View.GONE
        btnEditProfilePhoto.visibility = View.GONE
        btnEditInfo.visibility = View.GONE
        btnManageProperties.visibility = View.GONE
        
        // Keep Search Bar VISIBLE (as requested)
        etSearch.visibility = View.VISIBLE 
        
        // Hide list initially
        rvSearchResults.visibility = View.GONE
        
        // CONFIGURE TOP BAR FOR NAVIGATION
        // Change Menu Button to Back Button
        btnMenu.visibility = View.VISIBLE
        btnMenu.setImageResource(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        btnMenu.setOnClickListener {
            // Navigate back
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // Disable listeners that shouldn't work
        tvProfileName.setOnLongClickListener(null)
        tvUsername.setOnClickListener(null)
        
        setupSearchLogic() // Ensure search works here too
    }

    private fun configureEditMode() {
        // Show Search Bar
        etSearch.visibility = View.VISIBLE
        
        // Restore Menu Icon
        btnMenu.visibility = View.VISIBLE
        btnMenu.setImageResource(R.drawable.ic_menu)
        
        // Set Menu Click Listener (Navigation Drawer) WITH SEARCH CHECK
        btnMenu.setOnClickListener {
            
            // --- NEW: Check if Search Mode is Active ---
            if (isSearchMode) {
                exitSearchMode()
                return@setOnClickListener
            }
            // -------------------------------------------

            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawer_layout)
            val navView = requireActivity().findViewById<NavigationView>(R.id.nav_view)

            if (navView != null) {
                navView.menu.clear()
                if (navView.headerCount > 0) {
                    navView.removeHeaderView(navView.getHeaderView(0))
                }
                val headerView = navView.inflateHeaderView(R.layout.nav_drawer_custom_layout)

                currentUser?.let { user ->
                    val firstName = user.firstName.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    val lastName = user.lastName.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    headerView.findViewById<TextView>(R.id.drawer_user_name)?.text = "$firstName $lastName"
                    val headerImage = headerView.findViewById<ImageView>(R.id.drawer_profile_photo)
                    if (!user.profileImageUrl.isNullOrEmpty() && headerImage != null) {
                        Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_default_profile).into(headerImage)
                    }
                }

                val navItems = listOf(
                    headerView.findViewById<View>(R.id.nav_profile_info),
                    headerView.findViewById<View>(R.id.nav_privacy_security),
                    headerView.findViewById<View>(R.id.nav_account_delete),
                    headerView.findViewById<View>(R.id.nav_app_settings)
                )

                fun clearSelection() {
                    navItems.forEach { it?.isSelected = false }
                }

                navItems.forEachIndexed { index, item ->
                    item?.setOnClickListener {
                        clearSelection()
                        item.isSelected = true
                        when (index) {
                            0 -> startActivity(Intent(requireContext(), ProfileInfoActivity::class.java))
                            1 -> startActivity(Intent(requireContext(), AccountPrivacyActivity::class.java))
                            2 -> startActivity(Intent(requireContext(), AccountDeleteActivity::class.java))
                            3 -> startActivity(Intent(requireContext(), AppSettingsActivity::class.java))
                        }
                        drawerLayout?.closeDrawer(GravityCompat.START)
                    }
                }

                headerView.findViewById<View>(R.id.nav_logout)?.setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Log Out")
                        .setMessage("Are you sure you want to log out?")
                        .setIcon(R.drawable.ic_lock_power_off)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Log Out") { _, _ ->
                            val sharedPrefsHelper = SharedPreferencesHelper(requireContext())
                            sharedPrefsHelper.clear()
                            val intent = Intent(requireContext(), LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finishAffinity()
                        }
                        .show()
                }
                navItems.getOrNull(0)?.isSelected = true
            }
            drawerLayout?.openDrawer(GravityCompat.START)
        }

        setupSearchLogic()
        
        // Listeners specific to Edit Mode
        tvProfileName.setOnLongClickListener {
            viewModel.switchRole()
            true
        }
        
        tvUsername.setOnClickListener {
            val intent = Intent(requireContext(), ProfileInfoActivity::class.java)
            startActivity(intent)
        }

        btnEditProfilePhoto.setOnClickListener {
            isEditingProfilePhoto = true
            showImageSourceDialog()
        }

        btnEditCoverPhoto.setOnClickListener {
            isEditingProfilePhoto = false
            showImageSourceDialog()
        }

        ivCoverPhoto.setOnClickListener {
            isEditingProfilePhoto = false
            showImageSourceDialog()
        }

        btnEditInfo.setOnClickListener {
            // Replaced Toast with Snackbar including Dismiss action
            Snackbar.make(
                requireView(),
                "You can edit your contact info and phone number publicity.",
                Snackbar.LENGTH_LONG
            ).setAction("Dismiss") {
                // Dimiss logic is handled automatically by Snackbar
            }.show()

            layoutEditContact.visibility = View.VISIBLE
            currentUser?.let { user ->
                etInstagram.setText(user.instagramLink)
                etFacebook.setText(user.facebookLink)
                etWhatsapp.setText(user.whatsappLink)
                etWebsite.setText(user.websiteLink)
                switchPhonePublic.isChecked = user.isPhonePublic
            }
        }

        btnCancelEdit.setOnClickListener {
            layoutEditContact.visibility = View.GONE
        }

        btnSaveInfo.setOnClickListener {
            val instagram = etInstagram.text.toString().trim()
            val facebook = etFacebook.text.toString().trim()
            val whatsapp = etWhatsapp.text.toString().trim()
            val website = etWebsite.text.toString().trim()
            val isPublic = switchPhonePublic.isChecked

            val finalInstagram = if (instagram.isEmpty()) null else instagram
            val finalFacebook = if (facebook.isEmpty()) null else facebook
            val finalWhatsapp = if (whatsapp.isEmpty()) null else whatsapp
            val finalWebsite = if (website.isEmpty()) null else website

            viewModel.updateSocialLinks(finalInstagram, finalFacebook, finalWhatsapp, finalWebsite, isPublic)
            layoutEditContact.visibility = View.GONE
        }

        btnManageProperties.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_LandlordAddPropertyFragment)
        }
    }

    private fun enableSearchMode() {
        if (!isSearchMode) {
            isSearchMode = true
            btnMenu.setImageResource(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
            // Tint back arrow to ensure visibility (Primary Color)
            btnMenu.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_primary), android.graphics.PorterDuff.Mode.SRC_IN)
        }
    }

    private fun exitSearchMode() {
        if (isSearchMode) {
            isSearchMode = false
            etSearch.setText("")
            etSearch.clearFocus()
            rvSearchResults.visibility = View.GONE
            
            // Hide Keyboard
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(etSearch.windowToken, 0)
            
            // Restore Hamburger
            btnMenu.setImageResource(R.drawable.ic_menu)
            btnMenu.clearColorFilter()
        }
    }

    private fun setupSearchLogic() {
        // Search Functionality (Enter/Search Buttons)
        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                
                // Clear any pending auto-search
                searchHandler.removeCallbacks(searchRunnable)
                
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                
                // Hide Keyboard
                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }
        
        // Enable Search Mode on Focus or Click
        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) enableSearchMode()
        }
        etSearch.setOnClickListener {
            enableSearchMode()
        }
        
        // NEW: Handle Click on Right Drawable (Search Icon)
        etSearch.setOnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (etSearch.compoundDrawables[DRAWABLE_RIGHT] != null) {
                    if (event.rawX >= (etSearch.right - etSearch.compoundDrawables[DRAWABLE_RIGHT].bounds.width() - etSearch.paddingEnd - 30)) { 
                        searchHandler.removeCallbacks(searchRunnable)
                        val query = etSearch.text.toString().trim()
                        if (query.isNotEmpty()) {
                            performSearch(query)
                            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.hideSoftInputFromWindow(v.windowToken, 0)
                        }
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
        
        // Clear search results when text is cleared AND AUTO SEARCH
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchHandler.removeCallbacks(searchRunnable)
                if (s.isNullOrEmpty()) {
                    rvSearchResults.visibility = View.GONE
                } else {
                    // Auto Search after 500ms
                    searchHandler.postDelayed(searchRunnable, 500)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performSearch(query: String) {
        viewModel.searchUsers(query)
    }
    
    private fun navigateToUserProfile(selectedUser: User) {
        val fragment = ProfileFragment()
        val args = Bundle()
        args.putString("userId", selectedUser.userId)
        fragment.arguments = args
        
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        if (isReadOnly && targetUserId != null) {
            viewModel.loadOtherUserProfile(targetUserId!!)
        } else {
            viewModel.loadProfileData()
        }
        
        // Reset Search Mode on Resume
        if (!isReadOnly && isSearchMode) {
             exitSearchMode()
        }
        
        // RESTORED: Side Drawer Setup for Profile
        if (!isReadOnly) {
             val navView = activity?.findViewById<NavigationView>(R.id.nav_view)
             if (navView != null) {
                 navView.menu.clear()
                 if (navView.headerCount > 0) {
                     navView.removeHeaderView(navView.getHeaderView(0))
                 }
                 navView.inflateHeaderView(R.layout.nav_drawer_custom_layout)
             }
        }
    }

    // ... Helper functions ...
    private fun showImageSourceDialog() {
        val optionsList = mutableListOf("Take Photo", "Choose from Gallery")
        if (isEditingProfilePhoto) {
            if (!currentUser?.profileImageUrl.isNullOrEmpty()) optionsList.add("Remove Photo")
        } else {
            if (!currentUser?.coverImageUrl.isNullOrEmpty()) optionsList.add("Remove Photo")
        }
        val options = optionsList.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                val selection = options[which]
                when (selection) {
                    "Take Photo" -> checkCameraPermissionAndLaunch()
                    "Choose from Gallery" -> pickImageLauncher.launch("image/*")
                    "Remove Photo" -> {
                        if (isEditingProfilePhoto) viewModel.removeProfilePhoto()
                        else viewModel.removeCoverPhoto()
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> launchCamera()
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(requireContext(), "Camera permission is needed.", Toast.LENGTH_LONG).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val tempFile = File.createTempFile("temp_image", ".jpg", requireContext().cacheDir)
            tempImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", tempFile)
            takePictureLauncher.launch(tempImageUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error launching camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleImageSelection(uri: Uri) {
        if (isEditingProfilePhoto) {
            ivProfilePhoto.setImageURI(uri)
            currentUser?.let { user -> viewModel.updateProfile(user, uri) }
        } else {
            ivCoverPhoto.setImageURI(uri)
            currentUser?.let { user -> viewModel.updateCoverImage(user, uri) }
        }
    }

    private fun animateAndOpenUrl(view: View, url: String?) {
        if (url.isNullOrEmpty()) return
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat("scaleX", 1.2f), PropertyValuesHolder.ofFloat("scaleY", 1.2f)).apply { duration = 150 }
        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat("scaleX", 1f), PropertyValuesHolder.ofFloat("scaleY", 1f)).apply { duration = 150 }
        AnimatorSet().apply { playSequentially(scaleUp, scaleDown); start() }
        try {
            val finalUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(user: User) {
        val firstName = user.firstName.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val lastName = user.lastName.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        tvProfileName.text = "$firstName $lastName"
        val displayUsername = if (!user.username.isNullOrEmpty()) "@${user.username}" else "@$firstName"
        tvUsername.text = displayUsername
        tvUsername.visibility = if (displayUsername != "@") View.VISIBLE else View.GONE
        tvProfileBio.text = user.bio.ifEmpty { "No bio provided." }
        tvLocation.text = if (user.city.isNotEmpty()) "Lives in ${user.city}" else "Location not set"
        if (user.address.isNotEmpty()) {
            tvAddress.text = user.address
            tvAddress.visibility = View.VISIBLE
        } else {
            tvAddress.visibility = View.GONE
        }
        tvEmailContact.text = user.email
        if (user.isPhonePublic) {
            tvPhoneContact.visibility = View.VISIBLE
            tvPhoneContact.text = user.mobileNumber
        } else {
            tvPhoneContact.visibility = View.GONE
        }
        if (!user.instagramLink.isNullOrEmpty()) {
            ivInstagram.visibility = View.VISIBLE
            ivInstagram.setOnClickListener { animateAndOpenUrl(it, user.instagramLink) }
        } else {
            ivInstagram.visibility = View.GONE
        }
        if (!user.facebookLink.isNullOrEmpty()) {
            ivFacebook.visibility = View.VISIBLE
            ivFacebook.setOnClickListener { animateAndOpenUrl(it, user.facebookLink) }
        } else {
            ivFacebook.visibility = View.GONE
        }
        if (!user.whatsappLink.isNullOrEmpty()) {
            ivWhatsapp.visibility = View.VISIBLE
            ivWhatsapp.setOnClickListener { 
                var number = user.whatsappLink ?: ""
                number = number.replace(Regex("[^0-9+]"), "")
                if (number.startsWith("0")) number = "94" + number.substring(1)
                number = number.replace("+", "")
                val url = "https://wa.me/$number"
                animateAndOpenUrl(it, url) 
            }
        } else {
            ivWhatsapp.visibility = View.GONE
        }
        if (!user.websiteLink.isNullOrEmpty()) {
            ivWebsite.visibility = View.VISIBLE
            ivWebsite.setOnClickListener { animateAndOpenUrl(it, user.websiteLink) }
        } else {
            ivWebsite.visibility = View.GONE
        }
        if (!user.profileImageUrl.isNullOrEmpty()) {
            Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_default_profile).into(ivProfilePhoto)
        } else {
            ivProfilePhoto.setImageResource(R.drawable.ic_default_profile)
        }
        if (!user.coverImageUrl.isNullOrEmpty()) {
            Glide.with(this).load(user.coverImageUrl).placeholder(R.drawable.bg_login_header).into(ivCoverPhoto)
        } else {
            ivCoverPhoto.setImageResource(R.drawable.bg_login_header)
        }
        val role = user.role.trim()
        tvUserRole.text = role
        tvUserRole.visibility = View.VISIBLE
        val colorPrimary = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        val colorVerified = ContextCompat.getColor(requireContext(), R.color.verified_green)
        if (role.equals("Landlord", ignoreCase = true)) {
            tvUserRole.setTextColor(colorPrimary)
            ivRoleIcon.visibility = View.VISIBLE
            ivRoleIcon.setImageResource(R.drawable.ic_check_circle_filled)
            ivRoleIcon.setColorFilter(colorVerified)
            btnManageProperties.visibility = if (isReadOnly) View.GONE else View.VISIBLE
        } else {
            btnManageProperties.visibility = View.GONE
            tvUserRole.setTextColor(colorPrimary)
            ivRoleIcon.visibility = View.VISIBLE
            ivRoleIcon.setImageResource(R.drawable.ic_check_circle_filled)
            ivRoleIcon.setColorFilter(colorVerified)
        }
    }
}