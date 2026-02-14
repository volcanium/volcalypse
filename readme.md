# 🚀 Volcalypse - Advanced Missile System for Minecraft

A powerful and visually stunning Minecraft plugin that adds realistic missile systems with devastating effects, interceptor defenses, and persistent radioactive zones.

## ✨ Features

### 🎯 Missile Types

- **Small Missile** - Basic explosive with moderate damage
- **Medium Missile** - Enhanced explosive with shockwave effects and 30-minute contamination zone
- **Large Missile** - Massive explosion with multiple shockwaves and extended contamination
- **Nuclear Missile** - Apocalyptic explosion with mushroom cloud, radiation effects, and persistent contamination
- **Antimaterial Missile** - Dimensional implosion that completely vaporizes terrain in a 50-block radius
- **Incendiary Missile** - Creates widespread fires with lingering smoke and burn effects

### 🛡️ Defense System

- **Interceptor Levels 1-4**: Place defensive armor stands that can shoot down incoming missiles
  - Level 1: 10% interception chance
  - Level 2: 30% interception chance
  - Level 3: 50% interception chance
  - Level 4: 80% interception chance
- Spectacular laser beam effects when intercepting missiles
- Cannot intercept antimaterial missiles (they're unstoppable!)

### ☢️ Contamination Zones

- Explosions (except small) create **30-minute radioactive zones**
- Different hazard effects per zone type:
  - **Nuclear**: Poison IV, Nausea III, Wither III, Slowness II, Hunger IV
  - **Antimaterial**: Poison V, Nausea IV, Wither IV, Darkness II
  - **Incendiary**: Poison II, Nausea II, Fire damage
  - **Medium/Large**: Poison and Nausea effects
- Persistent particle effects mark contaminated areas
- Automatic cleanup after 30 minutes

### 🎨 Visual Effects

- Realistic missile trails with spiral particle effects
- Massive explosion animations with shockwaves
- Mushroom cloud formations for nuclear/antimaterial missiles
- Dimensional vortex effects for antimaterial explosions
- Sound effects for launch, flight, and detonation
- Persistent particle effects in contaminated zones

## 📋 Requirements

- **Minecraft**: 1.21+
- **Server**: Spigot or Paper
- **Java**: 21

## 🔧 Installation

1. Download the latest `volcalypse-1.0-SNAPSHOT.jar` from [Releases](../../releases)
2. Place the JAR file in your server's `plugins/` folder
3. Restart your server
4. Configure permissions (optional)

## 🎮 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/missile <type> <x> <z>` | Launch a missile to coordinates | `volcalypse.missile` (OP) |
| `/interceptor <level>` | Get an interceptor armor stand (1-4) | `volcalypse.interceptor` (OP) |
| `/removeinterceptor <radius>` | Remove all interceptors in radius | `volcalypse.removeinterceptor` (OP) |

### Missile Types
- `small` - Basic explosive
- `medium` - Enhanced explosive with contamination
- `large` - Massive explosive with extended contamination
- `nuclear` - Nuclear explosion with mushroom cloud
- `antimaterial` - Dimensional implosion (terrain destroyer)
- `incendiary` - Fire-spreading explosive

## 📖 Usage Examples

```bash
# Launch a nuclear missile to coordinates X:100, Z:-200
/missile nuclear 100 -200

# Get a Level 3 interceptor
/interceptor 3

# Remove all interceptors within 50 blocks
/removeinterceptor 50
```

### Setting Up Defenses

1. Obtain an interceptor: `/interceptor 3`
2. Right-click on a block to place the interceptor armor stand
3. The interceptor will automatically attempt to shoot down nearby missiles
4. Higher level interceptors have better success rates

## ⚙️ Configuration

### plugin.yml
```yaml
name: Volcalypse
version: 1.0
main: fr.volcastaff.Volcalypse
api-version: '1.21'
commands:
  missile:
    description: Launch a missile
    usage: /<command> <type> <x> <z>
  interceptor:
    description: Get an interceptor
    usage: /<command> <level>
  removeinterceptor:
    description: Remove interceptors in radius
    usage: /<command> <radius>
```

## 🏗️ Building from Source

### Prerequisites
- JDK 21
- Maven or Gradle

### Maven
```bash
git clone https://github.com/yourusername/volcalypse
cd volcalypse
mvn clean package
```

The compiled JAR will be in `target/volcalypse-1.0-SNAPSHOT.jar`

### Gradle
```bash
git clone https://github.com/yourusername/volcalypse
cd volcalypse
gradle clean build
```

The compiled JAR will be in `build/libs/volcalypse-1.0-SNAPSHOT.jar`

## 🎯 Technical Details

### Contamination System
- Uses `ConcurrentHashMap` for thread-safe zone tracking
- Automatic cleanup task runs every second (20 ticks)
- Persistent data containers store interceptor levels and placer UUIDs
- Zone duration: 1,800,000ms (30 minutes)

### Performance Optimizations
- Asynchronous terrain destruction for antimaterial missiles (5,000 blocks/tick)
- Asynchronous fire placement for incendiary missiles (3,000 blocks/tick)
- Efficient particle spawning with optimized intervals
- Smart explosion distribution for nuclear missiles

### Missile Physics
- Launch altitude: Y=300
- Descent velocity: -0.5 blocks/tick
- Interception checks: Every 58 ticks (except antimaterial)
- Detection radius: 30 blocks

## 🐛 Known Issues

- Extremely large explosions may cause brief server lag
- Antimaterial missiles can destroy bedrock if not protected
- Fire spread from incendiary missiles respects game rules

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the GNU License - see the [LICENSE](LICENSE) file for details.

## ⚠️ Disclaimer

This plugin is designed for controlled server environments. The antimaterial missile can cause significant terrain destruction. Use responsibly and consider making backups before testing.

## 🙏 Credits

Created by Jijuoff and mokilasto 

Special thanks to:
- Mokilasto

## 📞 Support

- **Issues**: [GitHub Issues](../../issues)
- **Discussions**: [GitHub Discussions](../../discussions)

## 🗺️ Roadmap

- [ ] Customizable missile power through config
- [ ] Economy integration for missile costs
- [ ] Silo construction system
- [ ] Missile guidance system (follow player/entity)
- [ ] Multiple warhead support
- [ ] Bunker system for protection
- [ ] Alliance/faction warfare integration
- [ ] Custom particle effects configuration

---

⭐ **If you enjoy this plugin, please consider giving it a star!** ⭐
