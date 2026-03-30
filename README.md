# Blocker

Application Android personnelle pour lutter contre l'addiction aux applications mobiles.

## Concept

Blocker intercepte l'ouverture d'applications que tu as identifiees comme addictives (Instagram, TikTok, etc.) et affiche un ecran d'attente avec un compte a rebours avant de te laisser y acceder. L'idee : casser la pulsion d'ouverture automatique en imposant un delai de reflexion. Le concept vient de l'application "one sec" qui limite à une application dans sa version gratuite.

## Fonctionnalites

- **Detection en temps reel** des ouvertures d'applications via AccessibilityService
- **Overlay plein ecran** avec compte a rebours anime
- **Liste configurable** d'applications a bloquer (toutes les apps installees sont listees)
- **Timer reglable** de 5 a 60 secondes (defaut : 10s)
- **Bouton "Non, je reviens"** pour annuler l'ouverture et revenir au home
- **Stockage persistant** des preferences via DataStore

## Architecture technique

| Composant                    | Role                                                    |
| ---------------------------- | ------------------------------------------------------- |
| `AccessibilityService`       | Detecte quelle app passe au premier plan                |
| `TYPE_ACCESSIBILITY_OVERLAY` | Affiche l'overlay sans permission `SYSTEM_ALERT_WINDOW` |
| `DataStore`                  | Stocke les apps bloquees et la duree du timer           |
| `Jetpack Compose`            | Interface utilisateur (ecrans + overlay)                |

## Comment ca marche

1. Tu ouvres une app bloquee (ex: Instagram)
2. Le service detecte le package au premier plan
3. L'app est renvoyee au home immediatement
4. Un overlay plein ecran avec un countdown s'affiche
5. A la fin du timer, tu dois appuyer sur un boutton pour confirmer que tu veux bien aller sur l'app
6. Ou tu cliques "Non, je reviens" et tu restes au home

## Installation

1. Ouvrir le projet dans Android Studio
2. Build & Run sur un appareil Android (min API 24 / Android 7.0)
3. Dans l'app : regler le timer et selectionner les apps a bloquer
4. Aller dans **Parametres > Accessibilite > Blocker** et activer le service

## Permissions

| Permission                   | Raison                                         |
| ---------------------------- | ---------------------------------------------- |
| `QUERY_ALL_PACKAGES`         | Lister les applications installees             |
| `BIND_ACCESSIBILITY_SERVICE` | Detecter les changements d'app au premier plan |

## Stack

- Kotlin
- Jetpack Compose + Material 3
- DataStore Preferences
- Navigation Compose
- Min SDK 24, Target SDK 36


## Debug
sur windows
adb devices
adb logcat -d | Select-String -Pattern "FATAL|AndroidRuntime|blocker" -Context 0,20


## Contact

mathieusdev@gmail.com
