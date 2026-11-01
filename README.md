# TIA ECOUTE

Lecteur de musique Android moderne et animé (Kotlin, Jetpack Compose, Media3/ExoPlayer).

**Fonctions** : bibliothèque (titres, albums, artistes, dossiers), recherche et tri, lecture en arrière-plan avec
notification et écran de verrouillage, disque vinyle animé, spectre néon, fond animé aux couleurs de la pochette,
mini-lecteur, file d'attente, aléatoire / répétition, vitesse, minuteur de veille (avec fondu),
égaliseur + bass boost + virtualizer, favoris, playlists, plus écoutés / récents, partage, reprise de la dernière écoute.

**Disque réaliste & designs** : vinyle (rotation 33 tours/min avec inertie, reflets fixes, sillons), platine avec bras qui
suit la progression, CD irisé, cassette, pochette + vinyle, néon radial. Le disque réagit à la musique (tempo propre à
chaque titre, ou son réel si vous activez « Disque synchronisé sur le son réel » ; seul le niveau sonore est lu).

**Menu** : Thèmes (6 ambiances), Widgets, Égaliseur, Amplificateur de volume, Minuterie de sommeil, Mode conduite,
Paramètres, Fichiers et dossiers masqués, Paramètres de lecture, À propos.

## Compiler sur GitHub (depuis le téléphone)
1. Créez un dépôt, ajoutez les fichiers du projet (ou le .zip + `.github/workflows/build.yml`).
2. Onglet **Actions** → « Build TIA ECOUTE (APK) » → *Run workflow* (ou simple push).
3. Récupérez l'APK dans **Releases** (ou dans les *Artifacts* du run) et installez `TIA-ECOUTE.apk`.
