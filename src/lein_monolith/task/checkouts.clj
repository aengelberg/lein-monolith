(ns lein-monolith.task.checkouts
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [lein-monolith.dependency :as dep]
    [lein-monolith.task.util :as u]
    [leiningen.core.main :as lein])
  (:import
    java.io.File
    (java.nio.file
      Files
      LinkOption
      Paths)))


(defn- create-symlink!
  "Creates a link from the given source path to the given target."
  [source target]
  (Files/createSymbolicLink
    source target
    (make-array java.nio.file.attribute.FileAttribute 0)))


(defn- link-checkout!
  "Creates a checkout dependency link to the given subproject."
  [^File checkouts-dir subproject force?]
  (let [dep-root (io/file (:root subproject))
        dep-name (dep/project-name subproject)
        link-name (if (namespace dep-name)
                    (str (namespace dep-name) "~" (name dep-name))
                    (name dep-name))
        link-path (.toPath (io/file checkouts-dir link-name))
        target-path (.relativize (.toPath checkouts-dir) (.toPath dep-root))]
    (if (Files/exists link-path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
      ; Link file exists.
      (let [actual-target (Files/readSymbolicLink link-path)]
        (if (and (Files/isSymbolicLink link-path)
                 (= target-path actual-target))
          ; Link exists and points to target already.
          (lein/info "Link for" dep-name "is correct")
          ; Link exists but points somewhere else.
          (if force?
            ; Recreate link since force is set.
            (do (lein/warn "Relinking" dep-name "from"
                           (str actual-target) "to" (str target-path))
                (Files/delete link-path)
                (create-symlink! link-path target-path))
            ; Otherwise print a warning.
            (lein/warn "WARN:" dep-name "links to" (str actual-target)
                       "instead of" (str target-path)))))
      ; Link does not exist, so create it.
      (do (lein/info "Linking" dep-name "to" (str target-path))
          (create-symlink! link-path target-path)))))


(defn link
  "Create symlinks in the checkouts directory pointing to all internal
  dependencies in the current project."
  [project opts project-names]
  (let [[monolith subprojects] (u/load-monolith! project)
        dep-map (dep/dependency-map subprojects)
        selected-names (into #{}
                             (map (partial dep/resolve-name! (keys dep-map)))
                             project-names)
        projects-to-link (->
                           (map (comp dep/condense-name first)
                                (:dependencies project))
                           (cond->>
                             (or (:deep opts) (seq project-names))
                             (mapcat (partial dep/upstream-keys dep-map))

                             (seq selected-names)
                             (filter selected-names))
                           (->>
                             (distinct)
                             (keep subprojects)))
        checkouts-dir (io/file (:root project) "checkouts")]
    (when (empty? projects-to-link)
      (lein/abort (str "Couldn't find any projects to link matching: "
                       (str/join " " project-names))))
    ; Create checkouts directory if needed.
    (when-not (.exists checkouts-dir)
      (lein/info "Creating checkout directory" (str checkouts-dir))
      (.mkdir checkouts-dir))
    ; Check each dependency for internal projects.
    (doseq [subproject projects-to-link]
      (link-checkout! checkouts-dir subproject (:force opts)))))


(defn unlink
  "Remove the checkouts directory from a project."
  [project]
  (when-let [checkouts-dir (some-> (:root project) (io/file "checkouts"))]
    (when (.exists checkouts-dir)
      (lein/info "Removing checkout directory" (str checkouts-dir))
      (doseq [link (.listFiles checkouts-dir)]
        (lein/debug "Removing checkout link" (str link))
        (.delete ^File link))
      (.delete checkouts-dir))))
