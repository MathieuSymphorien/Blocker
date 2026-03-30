# Politique de confidentialité — UnScrolled

**Dernière mise à jour : 30 mars 2026**

## 1. Présentation

UnScrolled est une application Android conçue pour aider les utilisateurs à contrôler leur usage des applications mobiles. Cette politique de confidentialité explique quelles données sont collectées, comment elles sont utilisées et quels sont tes droits.

---

## 2. Données collectées

### 2.1 Données stockées localement sur ton appareil

UnScrolled collecte et stocke **uniquement sur ton appareil** les informations suivantes :

| Donnée | Utilisation |
|---|---|
| Noms des applications bloquées | Afficher et gérer ta liste d'apps bloquées |
| Nombre de scrolls par application | Statistiques de défilement dans les stats |
| Temps passé par application | Statistiques de temps d'écran |
| Nombre de tentatives d'ouverture | Statistiques de résistance |
| Paramètres (limite quotidienne, profils, planificateur) | Configuration personnalisée |

### 2.2 Ce que UnScrolled NE collecte PAS

- Aucune donnée n'est envoyée sur un serveur externe.
- Aucune donnée personnelle (nom, email, numéro de téléphone) n'est collectée.
- Aucune donnée de localisation n'est utilisée.
- Le contenu des applications que tu utilises n'est pas lu ni enregistré.

---

## 3. Utilisation du service d'accessibilité (AccessibilityService)

UnScrolled utilise l'API **AccessibilityService** d'Android pour fonctionner. Cette permission est nécessaire pour :

- **Détecter quelle application est au premier plan** afin d'afficher l'écran de blocage si l'app est dans ta liste.
- **Compter les événements de défilement** (`TYPE_VIEW_SCROLLED`) pour calculer les statistiques de scrolls.

> ⚠️ UnScrolled utilise l'AccessibilityService **uniquement** pour détecter les apps en cours d'utilisation et compter les scrolls. L'application **ne lit pas**, **ne modifie pas** et **ne transmet pas** le contenu affiché dans les autres applications.

Cette déclaration est conforme à la politique [Google Play sur l'utilisation restreinte des services d'accessibilité](https://support.google.com/googleplay/android-developer/answer/10964491).

---

## 4. Stockage des données

Toutes les données sont stockées **localement** sur ton appareil en utilisant :
- **Room Database** (base de données SQLite locale) pour les statistiques d'usage
- **DataStore Preferences** (fichier de préférences local) pour les paramètres

Aucune donnée n'est synchronisée dans le cloud, ni envoyée à des tiers.

---

## 5. Partage des données

UnScrolled **ne partage aucune donnée** avec des tiers. Il n'y a :
- Aucun service d'analytics (Firebase, Mixpanel, etc.)
- Aucune publicité
- Aucun SDK tiers qui collecte des données

---

## 6. Sécurité

Les données restant locales, leur sécurité dépend de la sécurité de ton appareil (code de verrouillage, chiffrement du stockage Android).

---

## 7. Suppression des données

Pour supprimer toutes les données de UnScrolled :
1. Ouvre **Paramètres Android → Applications → UnScrolled**
2. Appuie sur **Effacer les données** et **Effacer le cache**

Ou désinstalle simplement l'application.

---

## 8. Enfants

UnScrolled ne collecte aucune donnée personnelle et ne cible pas spécifiquement les enfants. L'application est adaptée à tous les âges.

---

## 9. Modifications de cette politique

En cas de modification de cette politique, la date de « Dernière mise à jour » sera actualisée. Les changements majeurs seront signalés dans les notes de mise à jour de l'application.

---

## 10. Contact

Pour toute question concernant cette politique de confidentialité :

**Email :** mathieusdev@gmail.com

---

*Cette politique s'applique uniquement à l'application UnScrolled publiée sur le Google Play Store.*
