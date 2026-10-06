import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../../api/errors'
import { useStartSession, type StartSessionRequest } from '../../api/workouts'

/** Start treningu; gdy inny trening trwa – przejście do niego (409 session_already_active). */
export function useStartWorkout() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const start = useStartSession()
  const run = async (request: StartSessionRequest) => {
    try {
      const session = await start.mutateAsync(request)
      navigate(`/workout/${session.id}`)
    } catch (e) {
      if (e instanceof ApiError && e.code === 'session_already_active' && typeof e.problem.activeSessionId === 'string') {
        if (window.confirm(t('workout.alreadyActive'))) navigate(`/workout/${e.problem.activeSessionId}`)
        return
      }
      throw e
    }
  }
  return { run, isPending: start.isPending, error: start.error }
}
