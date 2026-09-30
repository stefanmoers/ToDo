import { useState } from 'react'
import { TagCreateForm } from '../features/tags/TagCreateForm'
import { TagRow } from '../features/tags/TagRow'
import { useTagList } from '../features/tags/useTagList'
import { useAllProjects } from '../features/projects/useProjectList'
import { usePageTitle } from '../hooks/usePageTitle'
import type { Tag } from '../api/tags'

export default function TagsPage() {
  usePageTitle('Meine Tags')
  const { state, reload } = useTagList()
  const projects = useAllProjects()
  const [notice, setNotice] = useState<string>()

  function onCreated(tag: Tag) {
    setNotice(`Der Tag „${tag.name}“ wurde angelegt.`)
    reload()
  }

  function onRenamed(tag: Tag) {
    setNotice(`Der Tag wurde in „${tag.name}“ umbenannt.`)
    reload()
  }

  function onDeleted() {
    setNotice('Der Tag wurde gelöscht.')
    reload()
  }

  return (
    <>
      <h1>Meine Tags</h1>
      <p>Tags markieren Aufgaben unabhängig von Status oder Projekt und lassen sich in der Aufgabenliste filtern.</p>
      {notice && (
        <p role="status" className="notice">
          {notice}
        </p>
      )}
      <TagCreateForm onCreated={onCreated} />
      {state.kind === 'loading' && <p role="status">Tags werden geladen …</p>}
      {state.kind === 'error' && (
        <div role="alert">
          <p>Die Tags konnten nicht geladen werden. {state.message}</p>
          <button type="button" onClick={reload}>
            Erneut versuchen
          </button>
        </div>
      )}
      {state.kind === 'loaded' && (
        <>
          <h2>Persönliche Tags</h2>
          {state.tags.filter((t) => t.scope === 'PERSONAL').length === 0 ? (
            <p>Noch keine persönlichen Tags.</p>
          ) : (
            <ul className="task-list" aria-label="Persönliche Tags">
              {state.tags
                .filter((t) => t.scope === 'PERSONAL')
                .map((tag) => (
                  <TagRow key={tag.id} tag={tag} onRenamed={onRenamed} onDeleted={onDeleted} />
                ))}
            </ul>
          )}
          {projects.map((project) => {
            const projectTags = state.tags.filter((t) => t.projectId === project.id)
            if (projectTags.length === 0) return null
            return (
              <div key={project.id}>
                <h2>Tags in „{project.name}“</h2>
                <ul className="task-list" aria-label={`Tags in ${project.name}`}>
                  {projectTags.map((tag) => (
                    <TagRow key={tag.id} tag={tag} onRenamed={onRenamed} onDeleted={onDeleted} />
                  ))}
                </ul>
              </div>
            )
          })}
        </>
      )}
    </>
  )
}
