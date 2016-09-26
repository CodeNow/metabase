(ns metabase.models.permissions-group
  (:require [metabase.db :as db]
            [metabase.models.interface :as i]
            [metabase.util :as u]))

(i/defentity PermissionsGroup :permissions_group)


;;; ------------------------------------------------------------ Magic Groups Getter Fns ------------------------------------------------------------

(defn- group-fetch-fn [group-name]
  (memoize (fn []
             (or (db/select-one PermissionsGroup
                   :name group-name)
                 (db/insert! PermissionsGroup
                   :name group-name)))))

(def ^{:arglists '([])} ^metabase.models.permissions_group.PermissionsGroupInstance
  default
  "Fetch the `Default` permissions group, creating it if needed."
  (group-fetch-fn "Default"))

(def ^{:arglists '([])} ^metabase.models.permissions_group.PermissionsGroupInstance
  admin
  "Fetch the `Admin` permissions group, creating it if needed."
  (group-fetch-fn "Admin"))

(def ^{:arglists '([])} ^metabase.models.permissions_group.PermissionsGroupInstance
  metabot
  "Fetch the `MetaBot` permissions group, creating it if needed."
  (group-fetch-fn "MetaBot"))


;;; ------------------------------------------------------------ Validation ------------------------------------------------------------

(defn exists-with-name?
  "Does a `PermissionsGroup` with GROUP-NAME exist in the DB? (case-insensitive)"
  ^Boolean [group-name]
  {:pre [(u/string-or-keyword? group-name)]}
  (db/exists? PermissionsGroup
    :%lower.name (s/lower-case (name group-name))))

(defn- check-name-not-already-taken
  [group-name]
  (when (exists-with-name? group-name)
    (throw (ex-info "A group with that name already exists." {:status-code 400}))))

(defn- check-not-magic-group
  "Make sure we're not trying to edit/delete one of the magic groups, or throw an exception."
  [{id :id}]
  {:pre [(integer? id)]}
  (doseq [magic-group [(default)
                       (admin)
                       (metabot)]]
    (when (= id (:id magic-group))
      (throw (ex-info (format "You cannot edit or delete the '%s' permissions group!" (:name magic-group))
               {:status-code 400})))))


;;; ------------------------------------------------------------ Lifecycle ------------------------------------------------------------

(defn- pre-insert [{group-name :name, :as group}]
  (u/prog1 group
    (check-name-not-already-taken group-name)))

(defn- pre-cascade-delete [{id :id, :as group}]
  (check-not-magic-group group)
  (db/cascade-delete! 'Permissions                :group_id id)
  (db/cascade-delete! 'PermissionsGroupMembership :group_id id))

(defn- pre-update [{group-name :name, :as group}]
  (u/prog1 group
    (check-not-magic-group group)
    (when group-name
      (check-name-not-already-taken group-name))))

(u/strict-extend (class PermissionsGroup)
  i/IEntity (merge i/IEntityDefaults
                   {:pre-cascade-delete pre-cascade-delete
                    :pre-insert         pre-insert
                    :pre-update         pre-update}))


;;; ------------------------------------------------------------ Util Fns ------------------------------------------------------------


(defn members
  "Return `Users` that belong to PERMISSIONS-GROUP, ordered by their name (case-insensitive)."
  [{group-id :id}]
  {:pre [(integer? group-id)]}
  (db/query {:select    [:core_user.first_name
                         :core_user.last_name
                         :core_user.email
                         [:core_user.id :user_id]
                         [:permissions_group_membership.id :membership_id]]
             :from      [:core_user]
             :left-join [:permissions_group_membership [:= :core_user.id :permissions_group_membership.user_id]]
             :where     [:= :permissions_group_membership.group_id group-id]
             :order-by  [[:%lower.core_user.first_name :asc]
                         [:%lower.core_user.last_name :asc]]}))
