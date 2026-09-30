import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import RequireAuth from './components/RequireAuth'
import { AuthProvider } from './features/auth/AuthContext'
import LoginPage from './pages/LoginPage'
import NotFoundPage from './pages/NotFoundPage'
import ProfilePage from './pages/ProfilePage'
import RegisterPage from './pages/RegisterPage'
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
          <Route element={<RequireAuth />}>
            <Route index element={<TaskListPage />} />
            <Route path="aufgaben/neu" element={<TaskCreatePage />} />
            <Route path="aufgaben/:taskId" element={<TaskDetailPage />} />
            <Route path="aufgaben/:taskId/bearbeiten" element={<TaskEditPage />} />
            <Route path="profil" element={<ProfilePage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}
