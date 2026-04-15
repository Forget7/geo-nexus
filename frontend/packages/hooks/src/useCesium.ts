/**
 * Cesium特效管理器 - 支持各种视觉效果
 */

import * as Cesium from 'cesium'
import type { Viewer } from 'cesium'

export interface CesiumRef {
  viewer: Viewer | null
  ready: boolean
}

export interface ParticleSystemOptions {
  color?: Cesium.Color
  particleSize?: number
  emissionRate?: number
  lifetime?: number
  speed?: number
  imageUrl?: string
}

export interface PostProcessOptions {
  enabled?: boolean
  strength?: number
  radius?: number
  contrast?: number
  brightness?: number
  hue?: number
  saturation?: number
}

/**
 * Cesium特效管理器
 */
export class EffectManager {
  private viewer: Viewer
  private effects: Map<string, any> = new Map()
  private particleSystems: Map<string, Cesium.ParticleSystem> = new Map()
  
  constructor(viewer: Viewer) {
    this.viewer = viewer
  }
  
  /**
   * 添加雨天效果
   */
  addRainEffect(options: ParticleSystemOptions = {}) {
    const rainSystem = this.createParticleSystem({
      color: Cesium.Color.LIGHTBLUE.withAlpha(0.6),
      particleSize: 0.3,
      emissionRate: 1000,
      lifetime: 3.0,
      speed: 15.0,
      imageUrl: this.createCircleTexture(),
      ...options
    })
    
    // 添加到场景
    this.viewer.scene.primitives.add(rainSystem)
    this.particleSystems.set('rain', rainSystem)
    this.effects.set('rain', true)
    
    return rainSystem
  }
  
  /**
   * 添加雪天效果
   */
  addSnowEffect(options: ParticleSystemOptions = {}) {
    const snowSystem = this.createParticleSystem({
      color: Cesium.Color.WHITE.withAlpha(0.8),
      particleSize: 0.2,
      emissionRate: 2000,
      lifetime: 5.0,
      speed: 3.0,
      imageUrl: this.createSnowflakeTexture(),
      ...options
    })
    
    // 粒子向下落
    snowSystem.emitConstraint = new Cesium.EmitterConstraint(Cesium.ParticleEmitter.Box, {
      dimensions: new Cesium.Cartesian3(1000, 1000, 100)
    })
    
    this.viewer.scene.primitives.add(snowSystem)
    this.particleSystems.set('snow', snowSystem)
    this.effects.set('snow', true)
    
    return snowSystem
  }
  
  /**
   * 添加沙尘暴效果
   */
  addSandstormEffect(options: ParticleSystemOptions = {}) {
    const sandSystem = this.createParticleSystem({
      color: Cesium.Color.SAND.withAlpha(0.4),
      particleSize: 0.5,
      emissionRate: 500,
      lifetime: 8.0,
      speed: 20.0,
      imageUrl: this.createDustTexture(),
      ...options
    })
    
    this.viewer.scene.primitives.add(sandSystem)
    this.particleSystems.set('sandstorm', sandSystem)
    this.effects.set('sandstorm', true)
    
    return sandSystem
  }
  
  /**
   * 添加火焰效果
   */
  addFireEffect(position: Cesium.Cartesian3, options: ParticleSystemOptions = {}) {
    const fireSystem = this.createParticleSystem({
      color: Cesium.Color.RED.withAlpha(0.8),
      particleSize: 1.0,
      emissionRate: 200,
      lifetime: 2.0,
      speed: 5.0,
      imageUrl: this.createFireTexture(),
      ...options
    })
    
    // 发射器位置
    const emitter = new Cesium.CircleEmitter(2.0)
    emitter.radius = 0.5
    
    // 向上发射
    emitter.placementTypes = Cesium.ParticlePlacementType.VOLUME
    emitter.position = position
    
    fireSystem.emitter = emitter
    
    // 颜色随生命周期变化
    fireSystem.colorStepDelegate = (particle: any, dt: number) => {
      const progress = particle.life / particle.maxLife
      if (progress > 0.7) {
        particle.color = Cesium.Color.RED.withAlpha(1.0 - progress)
      } else if (progress > 0.4) {
        particle.color = Cesium.Color.ORANGE.withAlpha(1.0 - progress)
      } else {
        particle.color = Cesium.Color.YELLOW.withAlpha(1.0 - progress)
      }
    }
    
    this.viewer.scene.primitives.add(fireSystem)
    this.particleSystems.set(`fire_${Date.now()}`, fireSystem)
    
    return fireSystem
  }
  
  /**
   * 添加爆炸效果
   */
  addExplosionEffect(position: Cesium.Cartesian3, options: {
    color?: Cesium.Color
    particleSize?: number
    maxParticles?: number
    intensity?: number
  } = {}) {
    const {
      color = Cesium.Color.ORANGE,
      particleSize = 2.0,
      maxParticles = 1000,
      intensity = 50
    } = options
    
    // 爆炸是一次性效果，不需要持续的emissionRate
    const explosionSystem = new Cesium.ParticleSystem({
      canvas: this.createParticleCanvas(),
      color: color.withAlpha(1.0),
      particleSize: particleSize,
      emissionRate: maxParticles,
      lifetime: 2.0,
      emitter: new Cesium.CircleEmitter(0.5),
      speed: intensity,
      minKeep: 0,
      maxKeep: 100,
      modelMatrix: Cesium.Transforms.eastNorthUpToFixedFrame(position),
      // 爆发模式
      burst: true,
      maxLife: 2.0,
      life: 2.0,
      // 渐变
      colorStepDelegate: (particle: any, dt: number) => {
        const progress = particle.life / particle.maxLife
        particle.color = color.withAlpha(progress)
        particle.size = particleSize * (1 - progress * 0.5)
      }
    })
    
    // 触发爆炸
    explosionSystem.burst = new Cesium.ParticleBurst({
      times: [0],
      minimumImageSize: new Cesium.Cartesian2(particleSize * 2, particleSize * 2),
      maximumImageSize: new Cesium.Cartesian2(particleSize * 4, particleSize * 4)
    })
    
    this.viewer.scene.primitives.add(explosionSystem)
    
    // 2秒后自动移除
    setTimeout(() => {
      this.viewer.scene.primitives.remove(explosionSystem)
    }, 2000)
    
    return explosionSystem
  }
  
  /**
   * 添加喷泉效果
   */
  addFountainEffect(position: Cesium.Cartesian3, options: ParticleSystemOptions = {}) {
    const fountainSystem = this.createParticleSystem({
      color: Cesium.Color.CYAN.withAlpha(0.7),
      particleSize: 0.3,
      emissionRate: 300,
      lifetime: 4.0,
      speed: 10.0,
      imageUrl: this.createWaterTexture(),
      ...options
    })
    
    // 圆形发射器
    const emitter = new Cesium.CircleEmitter(3.0)
    emitter.radius = 1.0
    
    // 向外喷射
    emitter.placementTypes = Cesium.ParticlePlacementType.VOLUME
    
    // 设置发射器位置
    fountainSystem.emitter = emitter
    fountainSystem.modelMatrix = Cesium.Transforms.eastNorthUpToFixedFrame(position)
    
    // 水流向上然后下落
    fountainSystem.forces = [(particle: any, dt: number) => {
      // 添加重力效果
      particle.velocity.y -= 9.8 * dt
    }]
    
    this.viewer.scene.primitives.add(fountainSystem)
    this.particleSystems.set(`fountain_${Date.now()}`, fountainSystem)
    
    return fountainSystem
  }
  
  /**
   * 添加烟雾效果
   */
  addSmokeEffect(position: Cesium.Cartesian3, options: ParticleSystemOptions = {}) {
    const smokeSystem = this.createParticleSystem({
      color: Cesium.Color.GRAY.withAlpha(0.4),
      particleSize: 2.0,
      emissionRate: 50,
      lifetime: 10.0,
      speed: 2.0,
      imageUrl: this.createSmokeTexture(),
      ...options
    })
    
    // 发射器
    const emitter = new Cesium.CircleEmitter(5.0)
    emitter.radius = 2.0
    emitter.placementTypes = Cesium.ParticlePlacementType.VOLUME
    
    smokeSystem.emitter = emitter
    smokeSystem.modelMatrix = Cesium.Transforms.eastNorthUpToFixedFrame(position)
    
    // 烟雾扩散和上升
    smokeSystem.forces = [(particle: any, dt: number) => {
      // 向上飘动
      particle.velocity.y += 1.0 * dt
      // 横向扩散
      particle.velocity.x *= 0.99
      particle.velocity.z *= 0.99
    }]
    
    // 颜色渐变（从深灰到浅灰）
    smokeSystem.colorStepDelegate = (particle: any, dt: number) => {
      const progress = particle.life / particle.maxLife
      particle.color = Cesium.Color.GRAY.withAlpha(0.4 * (1 - progress))
      particle.size = particle.size * 1.01 // 逐渐变大
    }
    
    this.viewer.scene.primitives.add(smokeSystem)
    this.particleSystems.set(`smoke_${Date.now()}`, smokeSystem)
    
    return smokeSystem
  }
  
  /**
   * 添加火焰动画（持续燃烧）
   */
  addFlameAnimation(position: Cesium.Cartesian3, options: {
    color1?: Cesium.Color
    color2?: Cesium.Color
    color3?: Cesium.Color
    height?: number
    intensity?: number
  } = {}) {
    const {
      color1 = Cesium.Color.RED,
      color2 = Cesium.Color.ORANGE,
      color3 = Cesium.Color.YELLOW,
      height = 50,
      intensity = 1.0
    } = options
    
    // 创建多个火焰粒子系统形成火焰
    const flames: Cesium.ParticleSystem[] = []
    
    // 内焰（黄色）
    const innerFlame = this.createParticleSystem({
      color: color3.withAlpha(0.9),
      particleSize: 0.8,
      emissionRate: 100 * intensity,
      lifetime: 1.5,
      speed: 5.0,
      imageUrl: this.createFireTexture()
    })
    
    // 中焰（橙色）
    const midFlame = this.createParticleSystem({
      color: color2.withAlpha(0.8),
      particleSize: 1.2,
      emissionRate: 80 * intensity,
      lifetime: 2.0,
      speed: 4.0,
      imageUrl: this.createFireTexture()
    })
    
    // 外焰（红色）
    const outerFlame = this.createParticleSystem({
      color: color1.withAlpha(0.7),
      particleSize: 1.8,
      emissionRate: 50 * intensity,
      lifetime: 2.5,
      speed: 3.0,
      imageUrl: this.createFireTexture()
    })
    
    // 向上发射
    const emitter = new Cesium.CircleEmitter(2.0)
    emitter.radius = 0.3
    emitter.placementTypes = Cesium.ParticlePlacementType.VOLUME
    
    innerFlame.emitter = emitter
    midFlame.emitter = emitter
    outerFlame.emitter = emitter
    
    // 颜色变化
    innerFlame.colorStepDelegate = (particle: any) => {
      particle.color = color3.withAlpha(0.9)
    }
    midFlame.colorStepDelegate = (particle: any) => {
      particle.color = color2.withAlpha(0.8)
    }
    outerFlame.colorStepDelegate = (particle: any) => {
      particle.color = color1.withAlpha(0.7)
    }
    
    // 重力（火焰向上）
    const upwardForce = (particle: any, dt: number) => {
      particle.velocity.y += 3.0 * dt
    }
    
    innerFlame.forces = [upwardForce]
    midFlame.forces = [upwardForce]
    outerFlame.forces = [upwardForce]
    
    // 添加到场景
    this.viewer.scene.primitives.add(innerFlame)
    this.viewer.scene.primitives.add(midFlame)
    this.viewer.scene.primitives.add(outerFlame)
    
    flames.push(innerFlame, midFlame, outerFlame)
    
    // 添加光源
    const light = new Cesium.PointLight(position, {
      color: Cesium.Color.ORANGE,
      intensity: 2.0 * intensity,
      distance: 100
    })
    this.viewer.scene.addLight(light)
    
    return { flames, light }
  }
  
  /**
   * 添加导弹尾迹效果
   */
  addMissileTrail(startPosition: Cesium.Cartesian3, endPosition: Cesium.Cartesian3, duration: number = 5) {
    const trail: Cesium.ParticleSystem[] = []
    
    // 创建尾迹
    const missileTrail = this.createParticleSystem({
      color: Cesium.Color.WHITE.withAlpha(0.8),
      particleSize: 0.5,
      emissionRate: 100,
      lifetime: 2.0,
      speed: 1.0,
      imageUrl: this.createCircleTexture()
    })
    
    // 开始位置
    missileTrail.modelMatrix = Cesium.Transforms.eastNorthUpToFixedFrame(startPosition)
    
    // 颜色渐变
    missileTrail.colorStepDelegate = (particle: any) => {
      particle.color = Cesium.Color.WHITE.withAlpha(particle.life / particle.maxLife)
    }
    
    this.viewer.scene.primitives.add(missileTrail)
    trail.push(missileTrail)
    
    // 动画移动
    const cartographic = Cesium.Cartographic.fromCartesian(startPosition)
    const startLon = cartographic.longitude
    const startLat = cartographic.latitude
    const startHeight = cartographic.height
    
    const endCartographic = Cesium.Cartographic.fromCartesian(endPosition)
    const endLon = endCartographic.longitude
    const endLat = endCartographic.latitude
    const endHeight = endCartographic.height
    
    let progress = 0
    const interval = setInterval(() => {
      progress += 0.02 / duration
      
      if (progress >= 1) {
        clearInterval(interval)
        this.viewer.scene.primitives.remove(missileTrail)
        return
      }
      
      const lon = Cesium.Math.lerp(startLon, endLon, progress)
      const lat = Cesium.Math.lerp(startLat, endLat, progress)
      const height = Cesium.Math.lerp(startHeight, endHeight, progress) + Math.sin(progress * Math.PI) * 100
      
      const pos = Cesium.Cartesian3.fromRadians(lon, lat, height)
      missileTrail.modelMatrix = Cesium.Transforms.eastNorthUpToFixedFrame(pos)
    }, 20)
    
    return { trail, interval }
  }
  
  /**
   * 创建泛光后期处理
   */
  addBloomEffect(options: {
    enabled?: boolean
    strength?: number
    threshold?: number
    radius?: number
  } = {}) {
    const {
      enabled = true,
      strength = 0.5,
      threshold = 0.8,
      radius = 0.3
    } = options
    
    const bloom = Cesium.PostProcessStageLibrary.createBloomEffect()
    bloom.enabled = enabled
    bloom.uniforms.strength = strength
    bloom.uniforms.threshold = threshold
    bloom.uniforms.radius = radius
    
    this.viewer.scene.postProcessStages.add(bloom)
    this.effects.set('bloom', bloom)
    
    return bloom
  }
  
  /**
   * 创建景深效果
   */
  addDepthOfFieldEffect(options: {
    enabled?: boolean
    focalDistance?: number
    delta?: number
    sigma?: number
  } = {}) {
    const {
      enabled = true,
      focalDistance = 1000,
      delta = 20,
      sigma = 100
    } = options
    
    const dof = new Cesium.DepthOfFieldEffect(this.viewer.scene, {
      focalDistance: focalDistance,
      delta: delta,
      sigma: sigma
    })
    
    dof.enabled = enabled
    
    this.viewer.scene.postProcessStages.add(dof)
    this.effects.set('depthOfField', dof)
    
    return dof
  }
  
  /**
   * 创建夜视效果
   */
  addNightVisionEffect() {
    // 绿色色调
    const nightVision = new Cesium.PostProcessStage({
      fragmentShader: `
        uniform sampler2D colorTexture;
        varying vec2 v_textureCoordinates;
        void main() {
          vec4 color = texture2D(colorTexture, v_textureCoordinates);
          float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
          vec4 nightVision = vec4(0.0, gray, 0.0, color.a);
          // 添加噪点
          float noise = fract(sin(dot(v_textureCoordinates * 1000.0, vec2(12.9898, 78.233))) * 43758.5453);
          nightVision.rgb += noise * 0.1;
          gl_FragColor = nightVision;
        }
      `
    })
    
    nightVision.enabled = true
    this.viewer.scene.postProcessStages.add(nightVision)
    this.effects.set('nightVision', nightVision)
    
    return nightVision
  }
  
  /**
   * 创建热成像效果
   */
  addThermalVisionEffect() {
    const thermal = new Cesium.PostProcessStage({
      fragmentShader: `
        uniform sampler2D colorTexture;
        varying vec2 v_textureCoordinates;
        void main() {
          vec4 color = texture2D(colorTexture, v_textureCoordinates);
          float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
          
          // 热成像伪彩色
          vec3 thermal;
          if (gray < 0.25) {
            thermal = mix(vec3(0.0, 0.0, 0.5), vec3(0.0, 0.0, 1.0), gray * 4.0);
          } else if (gray < 0.5) {
            thermal = mix(vec3(0.0, 0.0, 1.0), vec3(0.0, 1.0, 0.0), (gray - 0.25) * 4.0);
          } else if (gray < 0.75) {
            thermal = mix(vec3(0.0, 1.0, 0.0), vec3(1.0, 1.0, 0.0), (gray - 0.5) * 4.0);
          } else {
            thermal = mix(vec3(1.0, 1.0, 0.0), vec3(1.0, 0.0, 0.0), (gray - 0.75) * 4.0);
          }
          
          gl_FragColor = vec4(thermal, color.a);
        }
      `
    })
    
    thermal.enabled = true
    this.viewer.scene.postProcessStages.add(thermal)
    this.effects.set('thermal', thermal)
    
    return thermal
  }
  
  /**
   * 创建马赛克效果
   */
  addMosaicEffect(pixelSize: number = 10) {
    const mosaic = new Cesium.PostProcessStage({
      fragmentShader: `
        uniform sampler2D colorTexture;
        uniform float pixelSize;
        varying vec2 v_textureCoordinates;
        void main() {
          vec2 texSize = vec2(textureSize(colorTexture, 0.0));
          vec2 px = floor(v_textureCoordinates * texSize / pixelSize);
          vec2 uv = px * pixelSize / texSize;
          vec4 color = texture2D(colorTexture, uv);
          gl_FragColor = color;
        }
      `,
      uniforms: {
        pixelSize: pixelSize
      }
    })
    
    mosaic.enabled = true
    this.viewer.scene.postProcessStages.add(mosaic)
    this.effects.set('mosaic', mosaic)
    
    return mosaic
  }
  
  /**
   * 移除指定效果
   */
  removeEffect(name: string) {
    // 粒子效果
    const particle = this.particleSystems.get(name)
    if (particle) {
      this.viewer.scene.primitives.remove(particle)
      this.particleSystems.delete(name)
    }
    
    // 后期处理效果
    const postEffect = this.effects.get(name)
    if (postEffect) {
      if (postEffect instanceof Cesium.PostProcessStage) {
        this.viewer.scene.postProcessStages.remove(postEffect)
      }
      this.effects.delete(name)
    }
  }
  
  /**
   * 清空所有特效
   */
  clearAll() {
    // 清除所有粒子系统
    this.particleSystems.forEach((system, name) => {
      this.viewer.scene.primitives.remove(system)
    })
    this.particleSystems.clear()
    
    // 清除后期处理效果
    this.effects.forEach((effect, name) => {
      if (effect instanceof Cesium.PostProcessStage) {
        this.viewer.scene.postProcessStages.remove(effect)
      }
    })
    this.effects.clear()
  }
  
  /**
   * 销毁管理器
   */
  destroy() {
    this.clearAll()
  }
  
  // ==================== 私有辅助方法 ====================
  
  /**
   * 创建粒子系统
   */
  private createParticleSystem(options: ParticleSystemOptions): Cesium.ParticleSystem {
    const {
      color = Cesium.Color.WHITE,
      particleSize = 1.0,
      emissionRate = 100,
      lifetime = 5.0,
      speed = 10.0,
      imageUrl
    } = options
    
    return new Cesium.ParticleSystem({
      canvas: this.createParticleCanvas(),
      color: color,
      particleSize: particleSize,
      emissionRate: emissionRate,
      lifetime: lifetime,
      emitter: new Cesium.CircleEmitter(0.5),
      speed: speed,
      minLife: lifetime * 0.8,
      maxLife: lifetime,
      minSpeed: speed * 0.5,
      maxSpeed: speed * 1.5,
      minSize: particleSize * 0.5,
      maxSize: particleSize * 1.5,
      image: imageUrl || this.createCircleTexture(),
      // 发射器
      emissionScope: new Cesium.BoundingSphere(Cesium.Cartesian3.ZERO, 1000),
    })
  }
  
  /**
   * 创建粒子画布
   */
  private createParticleCanvas(): HTMLCanvasElement {
    const canvas = document.createElement('canvas')
    canvas.width = 64
    canvas.height = 64
    const ctx = canvas.getContext('2d')!
    
    const gradient = ctx.createRadialGradient(32, 32, 0, 32, 32, 32)
    gradient.addColorStop(0, 'rgba(255, 255, 255, 1)')
    gradient.addColorStop(0.3, 'rgba(255, 255, 255, 0.8)')
    gradient.addColorStop(1, 'rgba(255, 255, 255, 0)')
    
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, 64, 64)
    
    return canvas
  }
  
  /**
   * 创建圆形纹理
   */
  private createCircleTexture(): HTMLCanvasElement {
    const canvas = document.createElement('canvas')
    canvas.width = 32
    canvas.height = 32
    const ctx = canvas.getContext('2d')!
    
    ctx.beginPath()
    ctx.arc(16, 16, 16, 0, Math.PI * 2)
    ctx.fillStyle = 'white'
    ctx.fill()
    
    return canvas
  }
  
  /**
   * 创建雪花纹理
   */
  private createSnowflakeTexture(): HTMLCanvasElement {
    const canvas = document.createElement('canvas')
    canvas.width = 32
    canvas.height = 32
    const ctx = canvas.getContext('2d')!
    
    // 画十字形雪花
    ctx.strokeStyle = 'white'
    ctx.lineWidth = 2
    
    ctx.beginPath()
    ctx.moveTo(16, 4)
    ctx.lineTo(16, 28)
    ctx.moveTo(4, 16)
    ctx.lineTo(28, 16)
    ctx.stroke()
    
    // 添加对角线
    ctx.beginPath()
    ctx.moveTo(7, 7)
    ctx.lineTo(25, 25)
    ctx.moveTo(25, 7)
    ctx.lineTo(7, 25)
    ctx.stroke()
    
    return canvas
  }
  
  /**
   * 创建火焰纹理
   */
  private createFireTexture(): HTMLCanvasElement {
    const canvas = document.createElement('canvas')
    canvas.width = 64
    canvas.height = 64
    const ctx = canvas.getContext('2d')!
    
    const gradient = ctx.createRadialGradient(32, 32, 0, 32, 32, 32)
    gradient.addColorStop(0, 'rgba(255, 255, 200, 1)')
    gradient.addColorStop(0.4, 'rgba(255, 200, 0, 0.8)')
    gradient.addColorStop(0.7, 'rgba(255, 100, 0, 0.5)')
    gradient.addColorStop(1, 'rgba(255, 0, 0, 0)')
    
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, 64, 64)
    
    return canvas
  }
  
  /**
   * 创建烟雾纹理
   */
  private createSmokeTexture(): HTMLCanvasElement {
    const canvas = document.createElement('canvas')
    canvas.width = 64
    canvas.height = 64
    const ctx = canvas.getContext('2d')!
    
    const gradient = ctx.createRadialGradient(32, 32, 0, 32, 32, 32)
    gradient.addColorStop(0, 'rgba(128, 128, 128, 0.5)')
    gradient.addColorStop(0.5, 'rgba(128, 128, 128, 0.3)')
    gradient.addColorStop(1, 'rgba(128, 128, 128, 0)')
    
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, 64, 64)
    
    return canvas
  }
  
  /**
   * 创建水滴纹理
   */
  private createWaterTexture(): HTMLCanvasElement {
    const canvas = document.createElement('canvas')
    canvas.width = 32
    canvas.height = 32
    const ctx = canvas.getContext('2d')!
    
    const gradient = ctx.createRadialGradient(16, 16, 0, 16, 16, 16)
    gradient.addColorStop(0, 'rgba(200, 230, 255, 0.8)')
    gradient.addColorStop(0.5, 'rgba(100, 180, 255, 0.5)')
    gradient.addColorStop(1, 'rgba(50, 100, 255, 0)')
    
    ctx.fillStyle = gradient
    ctx.beginPath()
    ctx.arc(16, 16, 16, 0, Math.PI * 2)
    ctx.fill()
    
    return canvas
  }
  
  /**
   * 创建尘土纹理
   */
  private createDustTexture(): HTMLCanvasElement {
    const canvas = document.createElement('canvas')
    canvas.width = 32
    canvas.height = 32
    const ctx = canvas.getContext('2d')!
    
    ctx.fillStyle = 'rgba(200, 180, 150, 0.5)'
    
    for (let i = 0; i < 8; i++) {
      const x = Math.random() * 32
      const y = Math.random() * 32
      const r = Math.random() * 4 + 1
      ctx.beginPath()
      ctx.arc(x, y, r, 0, Math.PI * 2)
      ctx.fill()
    }
    
    return canvas
  }
}
