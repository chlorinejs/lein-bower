(ns leiningen.bower
  (:require [leiningen.help :as help]
            [leiningen.core.main :as main]
            [leiningen.npm :refer
             [npm environmental-consistency transform-deps]]
            [cheshire.core :as json]
            [leiningen.npm.deps :refer [resolve-node-deps]]
            [leiningen.npm.process :refer [exec]]
            [robert.hooke]
            [leiningen.deps]
            [clojure.java.io :as io]))

(defn project->bowerrc
  [project]
  (json/generate-string
   {:directory (get-in project [:bower :directory] "resources/public/vendor")
    :scripts (get-in project [:bower :scripts] {})}))

(defn project->component
  [project]
  (json/generate-string
   {:name (project :name)
    :description (project :description)
    :version (project :version)
    :dependencies (transform-deps
                   (resolve-node-deps :bower-dependencies project))}))

(defn- locate-bower-executable
  [location]
  (let [file (io/as-file location)]
    (when (.exists file)
      (if (.isDirectory file)
        (let [executable (io/as-file (str location "/bin/bower"))]
          (when (.exists executable)
            (.getPath executable)))
        (.getPath file)))))

(defn- npm-location
  [project name]
  (str (get-in project [:npm :root] ".") "/node_modules/" name))

(defn- determine-bower-command
  [project bower-location install-missing-bower?]
  (or
   (and (string? bower-location) (locate-bower-executable bower-location))
   (and (= bower-location :local) (locate-bower-executable (npm-location project "bower")))
   (and (= bower-location :local) install-missing-bower?
        (do
          (npm project "install" "bower")
          (locate-bower-executable (npm-location project "bower"))))
   (and (= bower-location :local) :missing-local-bower)
   :default))

(defn- invoke
  [project & args]
  (let [command (determine-bower-command
                 project
                 (get-in project [:bower :location] :local)
                 (get-in project [:bower :install-missing-bower] true))]
    (if (= command :missing-local-bower)
      (println "No local Bower module found. Install Bower or configure `:bower :location`.")
      (do
        (when (= command :default)
          (println "Using default command `bower` in PATH..."))
        (let [cmd (if (= command :default) "bower" command)]
          (try
            (exec (project :root) (cons cmd args))
            (catch java.io.IOException e
              (println "Failed to execute bower command:")
              (println "  " cmd)
              (println "Check your bower installation and its file permissions."))))))))

(defn- root [project]
  (if-let [root (get-in project [:bower :root])]
    (if (keyword? root)
      (project root) ;; e.g. support using :target-path
      root)
    (project :root)))

(defn- project-file
  [filename project]
  (io/file (root project) filename))

(defn bower-package-file
  [project]
  (io/file (root project) (get-in project [:bower :package-file] "bower.json")))

(defn bower-config-file
  [project]
  (io/file (root project) (get-in project [:bower :config-file] ".bowerrc")))

(defn- write-ephemeral-file
  [file content]
  (doto file
    (spit content)
    (.deleteOnExit)))

(defmacro with-ephemeral-file
  [file content & forms]
  `(try
     (write-ephemeral-file ~file ~content)
     ~@forms
     (finally (.delete ~file))))

(defn bower-debug
  [project file generator]
  (with-ephemeral-file file (generator project)
    (println (str "lein-bower generated " file ":\n"))
    (println (slurp file))))

(defn bower
  "Invoke the Bower component manager."
  ([project]
     (environmental-consistency project (bower-package-file project) (bower-config-file project))
     (println (help/help-for "bower"))
     (main/abort))
  ([project & args]
     (environmental-consistency project)
     (cond (= ["pprint"] args)
           (do (bower-debug project (bower-package-file project) project->component)
               (println)
               (bower-debug project (bower-config-file project) project->bowerrc))
           :else
           (with-ephemeral-file
             (bower-package-file project) (project->component project)
             (with-ephemeral-file
               (bower-config-file project) (project->bowerrc project)
               (apply invoke project args))))))

(defn install-deps
  [project]
  (environmental-consistency project)
  (with-ephemeral-file
    (bower-package-file project) (project->component project)
    (with-ephemeral-file
      (bower-config-file project) (project->bowerrc project)
      (invoke project "run-script" "bower"))))

(defn wrap-deps
  [f & args]
  (apply f args)
  (install-deps (first args)))

(defn install-hooks []
  (robert.hooke/add-hook #'leiningen.deps/deps wrap-deps))
