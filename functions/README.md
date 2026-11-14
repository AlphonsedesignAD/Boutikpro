# Authentification BoutikPro par identifiant

L'application utilise des identifiants internes, sans adresse e-mail demandée aux utilisateurs.

## Déploiement requis une seule fois

1. Installer Firebase CLI et se connecter au projet Firebase BoutikPro.
2. Depuis la racine du dépôt, exécuter :

```bash
firebase functions:secrets:set ADMIN_BOOTSTRAP_PASSWORD
```

Saisir le mot de passe administrateur initial : 0022.

3. Déployer les fonctions et les règles :

```bash
cd functions
npm install
cd ..
firebase deploy --only functions,firestore:rules
```

Les fonctions exigent App Check. Pour l'APK debug, récupérer le jeton Debug App Check dans Logcat puis l'enregistrer dans Firebase Console > App Check > Apps > BoutikPro > Manage debug tokens. Pour une version distribuée, activer le fournisseur Play Integrity et enregistrer l'application Android dans App Check.

Le compte administrateur est créé automatiquement au premier login valide. Les comptes vendeurs sont créés par l'administrateur. Ils partagent le même espace de données de boutique.
