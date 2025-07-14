# JointMaker Plugin - v4.5 RANDOM HIGH SYSTEM UPDATE

## Latest Changes (v4.5) - Random High Effects

### 🎲 **NEW FEATURE: Random High Types**
- **Bad High (10%)**: Greening out with nausea and levitation effects (20 seconds)
- **Good High (20%)**: Chiefed hard with jump boost III and luck II (20 seconds)  
- **Normal High (70%)**: Standard cannabis effects as before
- **Dynamic Experience**: Each hit rolls independently for high type
- **Realistic Variation**: Mimics real-world cannabis effect unpredictability

### 🎭 **Enhanced Audio-Visual Feedback**
- **Bad High**: Ghast scream sound + sickly green spore particles
- **Good High**: Level up sound + happy villager hearts + enchant sparkles
- **Normal High**: Traditional fire charge sounds + smoke particles
- **Immersive Messaging**: Cannabis culture terminology and color-coded feedback
- **Server Logging**: Tracks high types for debugging and statistics

### 🎮 **Balanced Gameplay**
- **Risk vs Reward**: Occasional negative effects balanced by enhanced positive ones
- **Social Integration**: All high types work with existing contact high system
- **Strategic Depth**: Multiple hits create varied session experiences
- **Maintained Balance**: 70% normal highs ensure consistent base experience

---

## Previous Changes (v4.4) - Contact High System

### 🌬️ **NEW FEATURE: Contact High**
- **Active Contact High**: Nearby players (5 blocks) get weaker effects when someone smokes
- **Passive Contact High**: Very close players (2 blocks) occasionally get faint effects from high players  
- **Distance Scaling**: Effect strength decreases with distance (very close → close → nearby)
- **Social Feedback**: Chat messages inform players about contact high interactions
- **Realistic Effects**: Weaker and shorter duration than direct smoking

### 🎭 **Enhanced Social Gameplay**
- **Group Dynamics**: Encourages players to smoke together for shared effects
- **Visual Feedback**: Smoke particles appear around affected players
- **Audio Cues**: Subtle experience orb sound for contact high
- **Smart Scaling**: Effect strength based on joint potency and distance
- **Spam Prevention**: Passive effects occur occasionally to avoid message flood

### 🔧 **Technical Improvements**
- **Dual System**: Immediate strong effects + ongoing weak effects
- **Performance Optimized**: Minimal impact on server performance
- **Range-Based**: 3D distance calculation for realistic effect zones
- **Effect Stacking**: Natural integration with existing potion system

---

## Previous Changes (v4.3) - Realistic Lighting Animation

### 🔥 **Lighting Animation**
- **Two-Phase Experience**: Realistic lighter → smoking sequence
- **Lighting Phase**: 3-second authentic lighter animation with flame particles
- **Spark Effects**: Critical hit particles simulate lighter sparks  
- **Progressive Ignition**: Multi-stage lighting with sound effects
- **Enhanced Realism**: Flint & steel sounds + fire ambient sounds

---

## Previous Changes (v4.1-4.2) - Joint Durability System

### 🆕 **Joint Durability & Animation**
- **Multiple Hits**: Each joint now has **5 hits** instead of single use
- **Progressive Effects**: Effects get stronger/longer with more hits remaining
- **Visual Feedback**: Joint shows remaining hits in name and lore
- **Smart Consumption**: Only removed when completely finished
- **Smoking Animation**: 3-second smoking sequence with slowness effect

---

## Previous Changes (v4.0) - MC 1.21.7 Compatibility

### 1. **Maven Dependencies Updated**
- **Spigot API**: Updated from `1.20.1-R0.1-SNAPSHOT` to `1.21.3-R0.1-SNAPSHOT`
- **Java Version**: Updated from Java 17 to Java 21 (recommended for MC 1.21+)
- **Repository URLs**: Updated to use more reliable endpoints

### 2. **Plugin Configuration Updated**
- **plugin.yml**: Updated API version from `1.20` to `1.21`
- **Version**: Now `4.4-CONTACT-HIGH`

### 3. **Code API Changes Fixed**
- **PotionEffectType.SLOW** → **PotionEffectType.SLOWNESS** (renamed in 1.21)
- **Particle.SMOKE_NORMAL** → **Particle.SMOKE** (renamed in 1.21)
- **Particle.SMOKE_LARGE** → **Particle.LARGE_SMOKE** (renamed in 1.21)
- **Removed unused import**: `PlayerPickupItemEvent` (deprecated)

## Features Summary

### ✅ **Core Features**
- Automatic conversion of rotten flesh to joints
- Realistic lighting animation with flame/spark particles
- Enhanced smoking animation with durability system
- **NEW**: Random high system with bad/good/normal high types (10%/20%/70%)
- Contact high system affecting nearby players
- Progressive potion effects and visual feedback
- Continuous smoke particles while "high"
- Persistent data container system for joint identification

### 🎯 **Commands**
- `/testjoints` - Basic functionality test
- `/testjoints convert` - Force conversion test  
- `/testjoints give` - Get rotten flesh (auto-converts)
- `/testjoints joint` - Get joints directly with full durability

### 🔄 **Automatic Systems**
- Real-time rotten flesh conversion
- Player join conversion
- Inventory open conversion
- Old joint durability upgrade
- Comprehensive lighting + smoking animation system
- **NEW**: Active and passive contact high detection

### 🌐 **Social Features**
- **Contact High Zones**: 5-block active range, 2-block passive range
- **Effect Sharing**: Nearby players get weaker cannabis effects
- **Social Feedback**: Clear messaging about contact high interactions
- **Group Dynamics**: Enhanced multiplayer experience

## Installation
1. Stop your Minecraft server
2. Replace the old plugin JAR with `lucyplugin-1.0-SNAPSHOT.jar`
3. Start your server
4. **Random high system works immediately with existing joints**

## Compatibility
- **Minecraft Version**: 1.21.7 and compatible versions
- **Java Version**: Java 21+ (recommended)
- **Server Software**: Spigot, Paper, or compatible forks
- **Backwards Compatible**: Works with existing joint data
- **Multiplayer Optimized**: Designed for social server environments

## Testing Status
- ✅ All tests pass successfully
- ✅ Plugin compiles without errors
- ✅ Random high system fully functional
- ✅ Contact high system fully functional
- ✅ Lighting animation system working perfectly
- ✅ Durability system operating correctly
- ✅ Ready for production use on MC 1.21.7 servers

## Multiplayer Testing Recommendations
- Test random high types using `/testjoints joint` and smoking multiple joints
- Try group smoking sessions to see mixed high types + contact high interactions
- Experience the 10%/20%/70% distribution over multiple smoking sessions
- Verify audio-visual effects work correctly for each high type
