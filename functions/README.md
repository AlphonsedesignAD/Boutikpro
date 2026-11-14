# Authentification BoutikPro par identifiant

L'application utilise des identifiants internes. Aucun e-mail n'est demandé aux administrateurs ou aux vendeurs.

## Configuration initiale

Le projet Firebase doit utiliser le forfait Blaze pour déployer Cloud Functions. Ce forfait exige un compte de facturation. Les fonctions disposent toutefois d'un quota mensuel sans frais. Consultez les alertes de budget avant le déploiement.

1. Installer Firebase CLI et se connecter :

```bash
npm install -g firebase-tools
firebase login
```

2. Depuis la racine du dépôt, enregistrer le mot de passe administrateur :

```bash
firebase functions:secrets:set ADMIN_BOOTSTRAP_PASSWORD --project gen-lang-client-0602841838
```

Saisir exactement le mot de passe initial demandé : 0022.

3. Déployer les fonctions et les règles :

```bash
cd functions
npm install
cd ..
firebase deploy --project gen-lang-client-0602841838 --only functions,firestore:rules
```

## App Check

Les fonctions refusent les appels sans App Check valide.

- APK de test : ouvrir l'application une fois, récupérer le jeton Debug App Check dans Logcat, puis l'enregistrer dans Firebase Console > App Check > Apps > BoutikPro > Manage debug tokens.
- Version distribuée : activer Play Integrity dans App Check pour l'application Android.

## Fonctionnement

Le compte administrateur est créé automatiquement lors de la première connexion valide. Il se connecte avec l'identifiant admin et le mot de passe configuré ci-dessus.

L'administrateur crée les comptes vendeurs depuis l'icône de gestion des comptes. Il peut les activer ou les désactiver. Les vendeurs utilisent leur identifiant et leur mot de passe, partagent le stock, les produits et les ventes de la boutique, et ne peuvent pas modifier les produits ni créer d'autres comptes.
