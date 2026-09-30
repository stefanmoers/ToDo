import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import RequireAuth from './components/RequireAuth'
import { AuthProvider } from './features/auth/AuthContext'
import AdminUsersPage from './pages/AdminUsersPage'
import DashboardPage from './pages/DashboardPage'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import PasswordResetUnavailablePage from './pages/PasswordResetUnavailablePage'
import ProfilePage from './pages/ProfilePage'
import ProjectCreatePage from './pages/ProjectCreatePage'
import ProjectDetailPage from './pages/ProjectDetailPage'
import ProjectEditPage from './pages/ProjectEditPage'
import ProjectListPage from './pages/ProjectListPage'
import RegisterPage from './pages/RegisterPage'
import TagsPage from './pages/TagsPage'
import TaskCreatePage from './pages/TaskCreatePage'
import TaskDetailPage from './pages/TaskDetailPage'
import TaskEditPage from './pages/TaskEditPage'
import TaskListPage from './pages/TaskListPage'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<Layout />}>
          <Route path="anmelden" element={<LoginPage />} />
          <Route path="registrieren" element={<RegisterPage />} />
          <Route path="passwort-vergessen" element={<PasswordResetUnavailablePage />} />
          <Route element={<RequireAuth />}>
            <Route index element={<TaskListPage />} />
            <Route path="uebersicht" element={<DashboardPage />} />
            <Route path="aufgaben/neu" element={<TaskCreatePage />} />
            <Route path="aufgaben/:taskId" element={<TaskDetailPage />} />
            <Route path="aufgaben/:taskId/bearbeiten" element={<TaskEditPage />} />
            <Route path="projekte" element={<ProjectListPage />} />
            <Route path="projekte/neu" element={<ProjectCreatePage />} />
            <Route path="projekte/:projectId" element={<ProjectDetailPage />} />
            <Route path="projekte/:projectId/bearbeiten" element={<ProjectEditPage />} />
            <Route path="tags" element={<TagsPage />} />
            <Route path="profil" element={<ProfilePage />} />
            <Route path="administration" element={<AdminUsersPage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}
