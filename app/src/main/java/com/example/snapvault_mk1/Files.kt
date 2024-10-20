    package com.example.snapvault_mk1

    import android.animation.Animator
    import android.animation.AnimatorListenerAdapter
    import android.animation.ObjectAnimator
    import android.content.Intent
    import android.os.Bundle
    import android.view.View
    import android.widget.ImageView
    import android.widget.ScrollView
    import androidx.appcompat.app.AppCompatActivity
    import android.content.SharedPreferences

    class Files : AppCompatActivity() {

        private lateinit var homeIcon: ImageView
        private lateinit var fileIcon: ImageView
        private lateinit var createIcon: ImageView
        private lateinit var personIcon: ImageView
        private lateinit var uploadIcon: ImageView
        private lateinit var imageAddIcon: ImageView
        private lateinit var newFolderIcon: ImageView
        private lateinit var scrollView: ScrollView
        private lateinit var sharedPreferences: SharedPreferences

        // Variable to track the visibility state of the icons
        private var areIconsVisible = false

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_files)

            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)

            // Retrieve the stored username from SharedPreferences
            val username = sharedPreferences.getString("username", null)

            homeIcon = findViewById(R.id.home)
            fileIcon = findViewById(R.id.folder)
            createIcon = findViewById(R.id.create)
            personIcon = findViewById(R.id.person)
            uploadIcon = findViewById(R.id.tabs)
            imageAddIcon = findViewById(R.id.imageadd)
            newFolderIcon = findViewById(R.id.newfolder)
            scrollView = findViewById(R.id.scrollView)

            homeIcon.setOnClickListener {
                // Navigate back to WelcomeActivity
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
                finish() // Optional: Finish Files activity
            }

            createIcon.setOnClickListener {
                val intent = Intent(this, Createalbum::class.java)
                startActivity(intent)
            }

            personIcon.setOnClickListener {
                val intent = Intent(this, User::class.java)
                startActivity(intent)
            }

            // When "tabs" icon is clicked
            uploadIcon.setOnClickListener {
                // Toggle the visibility of the icons with animation
                if (areIconsVisible) {
                    // If icons are currently visible, hide them
                    hideIcons()
                } else {
                    // If icons are currently hidden, show them
                    showIcons()
                }

                // Toggle the state
                areIconsVisible = !areIconsVisible
            }
        }

        private fun showIcons() {
            // Scroll up the ScrollView by a specific amount (e.g., height of icons)
            scrollView.post {
                // Scroll up before showing icons
                scrollView.smoothScrollBy(0, imageAddIcon.height + newFolderIcon.height) // Scroll up

                // After scrolling is complete, show the icons with animations
                imageAddIcon.visibility = View.VISIBLE
                newFolderIcon.visibility = View.VISIBLE

                // Set the initial position below the view
                imageAddIcon.translationY = imageAddIcon.height.toFloat() // Start below
                newFolderIcon.translationY = newFolderIcon.height.toFloat() // Start below

                // Slide up animation for imageAddIcon
                ObjectAnimator.ofFloat(imageAddIcon, "translationY", 0f).apply {
                    duration = 300
                    start()
                }

                // Slide up animation for newFolderIcon
                ObjectAnimator.ofFloat(newFolderIcon, "translationY", 0f).apply {
                    duration = 300
                    start()
                }

                // Fade in animation
                ObjectAnimator.ofFloat(imageAddIcon, "alpha", 0f, 1f).apply {
                    duration = 300
                    start()
                }

                ObjectAnimator.ofFloat(newFolderIcon, "alpha", 0f, 1f).apply {
                    duration = 300
                    start()
                }
            }
        }



        private fun hideIcons() {
            // Slide down animation for imageAddIcon
            ObjectAnimator.ofFloat(imageAddIcon, "translationY", imageAddIcon.height.toFloat()).apply {
                duration = 300
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        imageAddIcon.visibility = View.GONE // Hide after animation
                    }
                })
                start()
            }

            // Slide down animation for newFolderIcon
            ObjectAnimator.ofFloat(newFolderIcon, "translationY", newFolderIcon.height.toFloat()).apply {
                duration = 300
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        newFolderIcon.visibility = View.GONE // Hide after animation
                    }
                })
                start()
            }

            // Fade out animation
            ObjectAnimator.ofFloat(imageAddIcon, "alpha", 1f, 0f).apply {
                duration = 300
                start()
            }

            ObjectAnimator.ofFloat(newFolderIcon, "alpha", 1f, 0f).apply {
                duration = 300
                start()
            }

            // Scroll down the ScrollView back to the original position
            scrollView.post {
                scrollView.smoothScrollBy(0, -(imageAddIcon.height + newFolderIcon.height)) // Scroll down
            }
        }
    }
