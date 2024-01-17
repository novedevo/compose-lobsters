/*
 * Copyright © 2021-2024 Harsh Shandilya.
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package dev.msfjarvis.claw.android.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.paging.compose.collectAsLazyPagingItems
import com.deliveryhero.whetstone.compose.injectedViewModel
import dev.msfjarvis.claw.android.MainActivity
import dev.msfjarvis.claw.android.R
import dev.msfjarvis.claw.android.SearchActivity
import dev.msfjarvis.claw.android.ui.datatransfer.DataTransferScreen
import dev.msfjarvis.claw.android.ui.decorations.ClawAppBar
import dev.msfjarvis.claw.android.ui.decorations.ClawNavigationBar
import dev.msfjarvis.claw.android.ui.decorations.ClawNavigationRail
import dev.msfjarvis.claw.android.ui.decorations.NavigationItem
import dev.msfjarvis.claw.android.ui.getActivity
import dev.msfjarvis.claw.android.ui.lists.DatabasePosts
import dev.msfjarvis.claw.android.ui.lists.NetworkPosts
import dev.msfjarvis.claw.android.ui.navigation.ClawNavigationType
import dev.msfjarvis.claw.android.ui.navigation.Destinations
import dev.msfjarvis.claw.android.ui.rememberPostActions
import dev.msfjarvis.claw.android.viewmodel.ClawViewModel
import dev.msfjarvis.claw.api.LobstersApi
import dev.msfjarvis.claw.common.comments.CommentsPage
import dev.msfjarvis.claw.common.comments.HTMLConverter
import dev.msfjarvis.claw.common.urllauncher.UrlLauncher
import dev.msfjarvis.claw.common.user.UserProfile
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LobstersPostsScreen(
  urlLauncher: UrlLauncher,
  htmlConverter: HTMLConverter,
  windowSizeClass: WindowSizeClass,
  setWebUri: (String?) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ClawViewModel = injectedViewModel(),
) {
  val hottestListState = rememberLazyListState()
  val newestListState = rememberLazyListState()
  val savedListState = rememberLazyListState()
  val navController = rememberNavController()
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val postActions = rememberPostActions(urlLauncher, navController, viewModel)
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination?.route
  val context = LocalContext.current

  val hottestPosts = viewModel.hottestPosts.collectAsLazyPagingItems()
  val newestPosts = viewModel.newestPosts.collectAsLazyPagingItems()
  val savedPosts by viewModel.savedPostsByMonth.collectAsState(persistentMapOf())

  val navigationType = ClawNavigationType.fromSize(windowSizeClass.widthSizeClass)

  val postIdOverride = context.getActivity()?.intent?.extras?.getString(MainActivity.NAVIGATION_KEY)
  LaunchedEffect(false) {
    if (postIdOverride != null) {
      navController.navigate(
        Destinations.Comments.route.replace(Destinations.Comments.PLACEHOLDER, postIdOverride)
      )
    }
  }

  val navItems =
    persistentListOf(
      NavigationItem(
        label = "Hottest",
        route = Destinations.Hottest.route,
        icon = Icons.Outlined.Whatshot,
        selectedIcon = Icons.Filled.Whatshot,
      ) {
        coroutineScope.launch { hottestListState.animateScrollToItem(index = 0) }
      },
      NavigationItem(
        label = "Newest",
        route = Destinations.Newest.route,
        icon = Icons.Outlined.NewReleases,
        selectedIcon = Icons.Filled.NewReleases,
      ) {
        coroutineScope.launch { newestListState.animateScrollToItem(index = 0) }
      },
      NavigationItem(
        label = "Saved",
        route = Destinations.Saved.route,
        icon = Icons.Outlined.FavoriteBorder,
        selectedIcon = Icons.Filled.Favorite,
      ) {
        coroutineScope.launch { savedListState.animateScrollToItem(index = 0) }
      },
    )

  Scaffold(
    topBar = {
      ClawAppBar(
        navigationIcon = {
          if (
            navController.previousBackStackEntry != null &&
              navItems.none { it.route == currentDestination }
          ) {
            IconButton(
              onClick = { if (!navController.popBackStack()) context.getActivity()?.finish() }
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back to previous screen",
              )
            }
          }
        },
        title = {
          if (navItems.any { it.route == currentDestination }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
              )
              Text(text = stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
            }
          }
        },
        actions = {
          var expanded by remember { mutableStateOf(false) }
          if (navItems.any { it.route == currentDestination }) {
            IconButton(onClick = { expanded = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = "Extra options")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
              DropdownMenuItem(
                text = { Text("Backup and restore") },
                onClick = {
                  navController.navigate(Destinations.DataTransfer.route)
                  expanded = false
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.ImportExport,
                    contentDescription = "Data transfer options",
                  )
                },
              )
              DropdownMenuItem(
                text = { Text("Search posts") },
                onClick = {
                  context.startActivity(Intent(context, SearchActivity::class.java))
                  expanded = false
                },
                leadingIcon = {
                  Icon(imageVector = Icons.Filled.Search, contentDescription = "Search posts")
                },
              )
            }
          }
        },
      )
    },
    bottomBar = {
      AnimatedVisibility(visible = navigationType == ClawNavigationType.BOTTOM_NAVIGATION) {
        ClawNavigationBar(
          navController = navController,
          items = navItems,
          isVisible = navItems.any { it.route == currentDestination },
        )
      }
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    modifier = modifier.semantics { testTagsAsResourceId = true },
  ) { paddingValues ->
    Row(modifier = Modifier.padding(paddingValues)) {
      AnimatedVisibility(visible = navigationType == ClawNavigationType.NAVIGATION_RAIL) {
        ClawNavigationRail(
          navController = navController,
          items = navItems,
          isVisible = navItems.any { it.route == currentDestination },
        )
      }

      NavHost(
        navController = navController,
        startDestination = Destinations.startDestination.route,
        // Make animations 2x faster than default specs
        enterTransition = { fadeIn(animationSpec = tween(350)) },
        exitTransition = { fadeOut(animationSpec = tween(350)) },
      ) {
        val uri = LobstersApi.BASE_URL
        composable(route = Destinations.Hottest.route) {
          setWebUri("https://lobste.rs/")
          NetworkPosts(
            lazyPagingItems = hottestPosts,
            listState = hottestListState,
            isPostSaved = viewModel::isPostSaved,
            isPostRead = viewModel::isPostRead,
            postActions = postActions,
          )
        }
        composable(route = Destinations.Newest.route) {
          setWebUri("https://lobste.rs/")
          NetworkPosts(
            lazyPagingItems = newestPosts,
            listState = newestListState,
            isPostSaved = viewModel::isPostSaved,
            isPostRead = viewModel::isPostRead,
            postActions = postActions,
          )
        }
        composable(route = Destinations.Saved.route) {
          setWebUri(null)
          DatabasePosts(items = savedPosts, listState = savedListState, postActions = postActions)
        }
        composable(
          route = Destinations.Comments.route,
          arguments = listOf(navArgument("postId") { type = NavType.StringType }),
          deepLinks =
            listOf(
              navDeepLink { uriPattern = "$uri/s/${Destinations.Comments.PLACEHOLDER}/.*" },
              navDeepLink { uriPattern = "$uri/s/${Destinations.Comments.PLACEHOLDER}" },
            ),
        ) { backStackEntry ->
          val postId =
            requireNotNull(backStackEntry.arguments?.getString("postId")) {
              "Navigating to ${Destinations.Comments.route} without necessary 'postId' argument"
            }
          setWebUri("https://lobste.rs/s/$postId")
          CommentsPage(
            postId = postId,
            postActions = postActions,
            htmlConverter = htmlConverter,
            getSeenComments = viewModel::getSeenComments,
            markSeenComments = viewModel::markSeenComments,
          )
        }
        composable(
          route = Destinations.User.route,
          arguments = listOf(navArgument("username") { type = NavType.StringType }),
          deepLinks = listOf(navDeepLink { uriPattern = "$uri/u/${Destinations.User.PLACEHOLDER}" }),
        ) { backStackEntry ->
          val username =
            requireNotNull(backStackEntry.arguments?.getString("username")) {
              "Navigating to ${Destinations.User.route} without necessary 'username' argument"
            }
          setWebUri("https://lobste.rs/u/$username")
          UserProfile(username = username, getProfile = viewModel::getUserProfile)
        }
        composable(route = Destinations.DataTransfer.route) {
          DataTransferScreen(
            context = context,
            importPosts = viewModel::importPosts,
            exportPosts = viewModel::exportPosts,
            exportPostsAsHtml = viewModel::exportPostsAsHtml,
            snackbarHostState = snackbarHostState,
          )
        }
      }
    }
  }
}
