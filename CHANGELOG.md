# Changelog - Volcalypse Plugin

## Version 1.0.4 - VRAI Correctif de compatibilité Minecraft 1.21.10

### 🔥 CORRECTIONS CRITIQUES
- **VRAIE CORRECTION**: Résolution définitive de l'erreur `IllegalArgumentException: missing required data class org.bukkit.Color`
- **Problème identifié**: La particule `Particle.FLASH` nécessite une signature de méthode DIFFÉRENTE
- Tous les appels à `Particle.FLASH` utilisent maintenant la bonne surcharge: `spawnParticle(Particle.FLASH, location, count)`
- Plus aucune erreur sur aucun type de missile !

### Détails techniques v1.0.4
L'erreur persistait car `Particle.FLASH` est une particule spéciale qui peut prendre un objet `Color` en option.
Quand on utilisait `world.spawnParticle(Particle.FLASH, loc, count, 0.0, 0.0, 0.0, 0.0)`, le compilateur Java 
choisissait la surcharge qui nécessite un objet Color, d'où l'erreur.

**Solution définitive**: Utiliser `world.spawnParticle(Particle.FLASH, location, count)` sans les paramètres offset/extra.

### Lignes corrigées dans Volcalypse.java
- Ligne 577 (Large missile)
- Ligne 651 (Nuclear missile)  
- Ligne 773 (Antimaterial missile)
- Ligne 962 (Incendiary missile)

### Missiles testés et validés
- ✅ Small missile - FONCTIONNE
- ✅ Medium missile - FONCTIONNE
- ✅ Large missile - FONCTIONNE (correction v1.0.4)
- ✅ Nuclear missile - FONCTIONNE (correction v1.0.4)
- ✅ Antimaterial missile - FONCTIONNE (correction v1.0.4)
- ✅ Incendiary missile - FONCTIONNE (correction v1.0.4)

### Compatibilité
- ✅ Minecraft 1.21.10
- ✅ Minecraft 1.21.11
- ✅ Paper/Spigot

---

## Version 1.0.3 - Premier essai (incomplet)

### Corrections
- Conversion des entiers en doubles pour les paramètres `spawnParticle()`
- **Note**: Cette version ne résolvait PAS complètement le problème

### Détails techniques v1.0.3
L'erreur se produisait car certains appels `spawnParticle()` avec des valeurs entières créaient une ambiguïté.
Solution appliquée: Conversion explicite de tous les paramètres numériques en `double`.

**PROBLÈME**: Cette correction n'était pas suffisante pour `Particle.FLASH`

---

### Installation
1. Supprimez l'ancienne version du plugin de votre dossier `plugins/`
2. Placez `volcalypse-1.0.4.jar` dans votre dossier `plugins/`
3. Redémarrez votre serveur

### Compilation
```bash
mvn clean package
```
Le fichier JAR sera généré dans `target/volcalypse-1.0.4.jar`
