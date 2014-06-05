(ns leiningen.bower
  (:require [leiningen.help :as help]
            [leiningen.core.main :as main]
            [leiningen.npm :refer
             [with-json-file environmental-consistency transform-deps]]
            [cheshire.core :as json]
            [leiningen.npm.deps :refer [resolve-node-deps]]
            [leiningen.npm.process :refer [exec]]
            [robert.hooke]
            [leiningen.deps]
            [clojure.java.io :as io]))

(defn project->bowerrc
  [project]
  (json/generate-string
   {:directory (get-in project [:bower :directory] "resources/public/vendor")}))

(defn project->component
  [project]
  (json/generate-string
   {:name (project :name)
    :description (project :description)
    :version (project :version)
    :dependencies (transform-deps
                   (resolve-node-deps :bower-dependencies project))}))

(defn- invoke
  [project & args]
  (let [local-bower (io/as-file "./node_modules/bower/bin/bower")
        cmd (if (.exists local-bower) (.getPath local-bower) "bower")]
    (exec (project :root) (cons cmd args))))

(defn bower-package-file
  [project]
  (get-in project [:bower :package-file] "bower.json"))

(defn bower-config-file
  [project]
  (get-in project [:bower :config-file] ".bowerrc"))

(defn bower-debug
  [project filename generator]
  (with-json-file filename (generator project) project
    (println (str "lein-bower generated " filename ":\n"))
    (println (slurp filename))))

(defn bower
  "Invoke the Bower component manager."
  ([project]
     (environmental-consistency project (bower-package-file project) (bower-config-file project))
     (println (help/help-for "bower"))
     (main/abort))
  ([project & args]
     (environmental-consistency project)
     (cond (= ["pprint"] args)
           (do (bower-debug project (bower-package-file project) project->bowerrc)
               (println)
               (bower-debug project (bower-config-file project) project->component))
           :else
           (with-json-file
             (bower-package-file project) (project->component project) project
             (with-json-file
               (bower-config-file project) (project->bowerrc project) project
               (apply invoke project args))))))

(defn install-deps
  [project]
  (environmental-consistency project)
  (with-json-file
    (bower-package-file project) (project->component project) project
    (with-json-file
      (bower-config-file project) (project->bowerrc project) project
      (invoke project "run-script" "bower"))))

(defn wrap-deps
  [f & args]
  (apply f args)
  (install-deps (first args)))

(defn install-hooks []
  (robert.hooke/add-hook #'leiningen.deps/deps wrap-deps))
