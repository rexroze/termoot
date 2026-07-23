package com.termoot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.termoot.ui.screens.editor.WorkspaceEditorScreen
import com.termoot.ui.screens.terminal.TerminalScreen
import com.termoot.ui.screens.workspaces.WorkspaceListScreen

object Routes {
    const val WORKSPACES = "workspaces"
    const val EDITOR = "editor?workspaceId={workspaceId}"
    const val TERMINAL = "terminal/{workspaceId}"

    fun editorRoute(workspaceId: String? = null): String =
        if (workspaceId != null) "editor?workspaceId=$workspaceId"
        else "editor"

    fun terminalRoute(workspaceId: String) = "terminal/$workspaceId"
}

@Composable
fun TermootNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.WORKSPACES
    ) {
        /* ── Workspace List ── */
        composable(Routes.WORKSPACES) {
            WorkspaceListScreen(
                onNavigateToEditor = { workspaceId ->
                    navController.navigate(Routes.editorRoute(workspaceId))
                },
                onNavigateToTerminal = { workspaceId ->
                    navController.navigate(Routes.terminalRoute(workspaceId))
                }
            )
        }

        /* ── Workspace Editor (Create / Edit) ── */
        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("workspaceId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val workspaceId = backStackEntry.arguments?.getString("workspaceId")
            WorkspaceEditorScreen(
                workspaceId = workspaceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        /* ── Terminal ── */
        composable(
            route = Routes.TERMINAL,
            arguments = listOf(
                navArgument("workspaceId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val workspaceId = backStackEntry.arguments?.getString("workspaceId")
                ?: return@composable
            TerminalScreen(
                workspaceId = workspaceId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
