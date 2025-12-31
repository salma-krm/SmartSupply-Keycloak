# SmartSupply-Keycloak
La sécurité est assurée par Keycloak, qui gère l’authentification des utilisateurs à l’aide d’une session serveur et délivre des JWT pour sécuriser l’accès aux API. L’application reste stateless, chaque requête étant autorisée uniquement via un token valide, sans gestion de session côté backend.
# 🔐 Configuration Keycloak - SmartSupply

## 📦 Contenu
Ce fichier contient la configuration complète du realm Keycloak **logistics-realm** incluant :
- ✅ Clients configurés (Spring Boot API, etc.)
- ✅ Rôles (realm et client)
- ✅ Scopes et mappings
- ✅ Paramètres de sécurité et authentification

## 🚀 Utilisation

### Pour importer la configuration automatiquement

Après avoir cloné le repository :

```bash
# 1. Cloner le projet (si pas déjà fait)
git clone https://github.com/salma-krm/SmartSupply-Keycloak.git
cd SmartSupply-Keycloak

# 2. Démarrer tous les services (l'import se fait automatiquement)
docker-compose up -d

# 3. Vérifier que Keycloak a bien démarré et importé la config
docker logs SmartSupply-keycloak

# 4. Accéder à la console admin
# URL: http://localhost:8090/admin
# Username: admin
# Password: admin
```

### ✅ Vérifier l'import

1. Connectez-vous à http://localhost:8090/admin
2. Dans le menu déroulant en haut à gauche, vérifiez que **logistics-realm** existe
3. Allez dans **Clients** pour voir les clients configurés
4. Allez dans **Realm roles** pour voir les rôles

## 🔄 Mettre à jour la configuration

Si vous modifiez la configuration Keycloak (ajout de client, rôle, etc.) :

### Sous Windows (PowerShell) :
```powershell
# Exporter la nouvelle configuration
docker exec -it SmartSupply-keycloak /opt/keycloak/bin/kc.sh export --dir /tmp/export --realm logistics-realm --users skip

# Copier le fichier mis à jour
docker cp SmartSupply-keycloak:/tmp/export/logistics-realm-realm.json ./keycloak-config/logistics-realm.json

# Committer les changements
git add keycloak-config/logistics-realm.json
git commit -m "chore: mise à jour configuration Keycloak"
git push
```

### Sous Linux/Mac (Bash) :
```bash
# Exporter la nouvelle configuration
docker exec -it SmartSupply-keycloak /opt/keycloak/bin/kc.sh export \
  --dir /tmp/export \
  --realm logistics-realm \
  --users skip

# Copier le fichier mis à jour
docker cp SmartSupply-keycloak:/tmp/export/logistics-realm-realm.json \
  ./keycloak-config/logistics-realm.json

# Committer les changements
git add keycloak-config/logistics-realm. json
git commit -m "chore: mise à jour configuration Keycloak"
git push
```

## ⚠️ Important
- ❌ Ne modifiez **jamais** manuellement le fichier JSON
- ✅ Utilisez toujours l'export Keycloak pour mettre à jour la configuration
- ✅ Committez régulièrement les changements de configuration
- ✅ Informez l'équipe quand vous modifiez la config Keycloak

## 🐛 Troubleshooting

### Le realm n'est pas importé
```bash
# Vérifier les logs de Keycloak
docker logs SmartSupply-keycloak

# Redémarrer avec import forcé
docker-compose down
docker-compose up -d
```

### Erreur "Realm already exists"
C'est normal !  Keycloak détecte que le realm existe déjà et ne le réimporte pas.
Pour réimporter :
```bash
docker-compose down -v  # ⚠️ Attention :  supprime aussi la base de données
docker-compose up -d
```
```