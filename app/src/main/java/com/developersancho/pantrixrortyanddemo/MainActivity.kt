package com.developersancho.pantrixrortyanddemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.developersancho.pantrixrortyanddemo.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navController = (supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment)
            .navController
        // Only the two tab destinations are top level; everything else gets an Up arrow.
        val appBarConfiguration = AppBarConfiguration(setOf(
            R.id.charactersFragment,
            R.id.episodesFragment,
            R.id.locationsFragment,
            R.id.labFragment
        ))
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = (supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment)
            .navController
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
