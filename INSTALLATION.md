# BoutikPro, installation gratuite

BoutikPro utilise Firebase Authentication et Cloud Firestore. Cette version n'utilise pas Cloud Functions, aucun forfait Blaze n'est nécessaire.

## 1. Activer la connexion

Dans Firebase Console, ouvrez le projet `gen-lang-client-0602841838`.

1. Ouvrez Authentication, puis Sign-in method.
2. Activez Email/Password.
3. Enregistrez.

L'application n'affiche pas d'adresse e-mail. Elle transforme les identifiants en adresses techniques internes se terminant par `@boutikpro.app`.

## 2. Créer le compte administrateur une seule fois

Dans Authentication, ouvrez Users, puis Add user.

- E-mail : `admin@boutikpro.app`
- Mot de passe Firebase : `002200`

Ce compte doit être créé manuellement dans Firebase Console. Ne créez pas un autre compte avec cette adresse.

Dans BoutikPro, connectez-vous ensuite avec :

- Identifiant : `admin`
- Mot de passe : `0022`

## 3. Publier les règles de sécurité gratuites

À la racine du dépôt, exécutez :

```bash
npm install -g firebase-tools
firebase login
firebase deploy --only firestore:rules --project gen-lang-client-0602841838
```

Cette commande publie uniquement les règles Firestore. Elle ne déploie aucune fonction et ne demande pas le forfait Blaze.

Vous pouvez aussi copier le contenu de `firestore.rules` dans Firestore Database > Rules dans Firebase Console, puis cliquer sur Publish.

## 4. Comptes vendeurs

Après la connexion administrateur, ouvrez l'icône de gestion des comptes.

L'administrateur peut créer et désactiver les vendeurs. Chaque vendeur se connecte avec son identifiant et son mot de passe. Aucun e-mail réel n'est demandé.

Tous les comptes actifs utilisent le même stock, les mêmes catégories et le même historique des ventes.

## Important

Le compte administrateur doit exister dans Firebase Authentication avant la première connexion. Les règles Firestore doivent être publiées avant d'utiliser l'application. Le projet Firebase et sa base Firestore existante sont conservés.
