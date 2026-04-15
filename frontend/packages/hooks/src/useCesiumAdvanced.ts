/**
 * Cesium高级特效 - 动态路径、3D动画、动态可视化
 */

import * as Cesium from 'cesium'
import type { Viewer, Entity, DataSource } from 'cesium'

export interface DynamicPathOptions {
  color?: Cesium.Color
  width?: number
  leadTime?: number
  trailTime?: number
  glow?: boolean
  pulsate?: boolean
}

export interface AnimatedEntityOptions {
  id: string
  position: Cesium.PositionProperty
  orientation?: Cesium.OrientationProperty
  model?: {
    uri: string
    scale?: number
    color?: Cesium.Color
    animationSpeed?: number
  }
  billboard?: {
    image: string
    scale?: number
    color?: Cesium.Color
  }
}

/**
 * Cesium高级动画管理器
 */
export class CesiumAnimationManager {
  private viewer: Viewer
  private animations: Map<string, Cesium.Animation> = new Map()
  private dynamicPaths: Map<string, Entity> = new Map()
  private entityAnimations: Map<string, Entity> = new Map()
  
  constructor(viewer: Viewer) {
    this.viewer = viewer
  }
  
  /**
   * 创建动态路径（飞机/车辆轨迹）
   */
  createDynamicPath(
    id: string,
    positions: Cesium.SampledPositionProperty[],
    options: DynamicPathOptions = {}
  ): Entity {
    const {
      color = Cesium.Color.CYAN,
      width = 3,
      leadTime = 0,
      trailTime = 3600,
      glow = true,
      pulsate = false
    } = options
    
    // 创建路径实体
    const pathEntity = this.viewer.entities.add({
      id: id,
      name: id,
      path: {
        resolution: 1,
        material: glow 
          ? new Cesium.PolylineGlowMaterialProperty({
              glowPower: 0.3,
              color: color.withAlpha(0.8)
            })
          : color.withAlpha(0.8),
        width: width,
        leadTime: leadTime,
        trailTime: trailTime
      },
      position: positions[0] || Cesium.Cartesian3.fromDegrees(0, 0, 0)
    })
    
    this.dynamicPaths.set(id, pathEntity)
    
    // 添加动画
    if (positions.length > 1) {
      this.animateAlongPath(id, positions, pulsate)
    }
    
    return pathEntity
  }
  
  /**
   * 路径动画
   */
  private animateAlongPath(
    entityId: string,
    positions: Cesium.SampledPositionProperty[],
    loop: boolean = true
  ) {
    const entity = this.dynamicPaths.get(entityId)
    if (!entity) return
    
    let time = 0
    const totalPositions = positions.length
    
    const animation = new Cesium.Animation({
      update: () => {
        if (!entity.path) return
        
        const index = Math.floor(time) % totalPositions
        const nextIndex = (index + 1) % totalPositions
        
        if (positions[index] && positions[nextIndex]) {
          // 插值位置
          const t = time % 1
          const pos1 = positions[index].getValue(Cesium.JulianDate.now())
          const pos2 = positions[nextIndex].getValue(Cesium.JulianDate.now())
          
          if (pos1 && pos2) {
            const interpolated = Cesium.Cartesian3.lerp(pos1, pos2, t, new Cesium.Cartesian3())
            entity.position = new Cesium.ConstantPositionProperty(interpolated)
          }
        }
        
        time += 0.01
      },
      loop: loop
    })
    
    this.animations.set(entityId, animation)
    animation.start()
  }
  
  /**
   * 创建飞机轨迹
   */
  createAircraftPath(
    id: string,
    waypoints: Array<{
      lon: number
      lat: number
      alt: number
      time: number // 相对时间（秒）
    }>,
    options: {
      color?: Cesium.Color
      showModel?: boolean
      modelUri?: string
    } = {}
  ): Entity {
    const {
      color = Cesium.Color.CYAN,
      showModel = true,
      modelUri = 'https://cesium.com/public/SandcastleSampleData/Cesium_Air.glb'
    } = options
    
    // 创建采样位置
    const position = new Cesium.SampledPositionProperty()
    
    waypoints.forEach(wp => {
      const time = Cesium.JulianDate.addSeconds(
        Cesium.JulianDate.now(),
        wp.time,
        new Cesium.JulianDate()
      )
      const cartesian = Cesium.Cartesian3.fromDegrees(wp.lon, wp.lat, wp.alt)
      position.addSample(time, cartesian)
    })
    
    // 计算航向
    const orientation = new Cesium.VelocityOrientationProperty(position)
    
    // 创建实体
    const entity = this.viewer.entities.add({
      id: id,
      name: '飞机',
      position: position,
      orientation: showModel ? orientation : undefined,
      model: showModel ? {
        uri: modelUri,
        scale: 1.0,
        minimumPixelSize: 32
      } : undefined,
      path: {
        resolution: 1,
        material: new Cesium.PolylineGlowMaterialProperty({
          glowPower: 0.3,
          color: color.withAlpha(0.6)
        }),
        width: 3,
        leadTime: 10,
        trailTime: 60
      },
      // 标签
      label: {
        text: '航班',
        font: '12px sans-serif',
        fillColor: Cesium.Color.WHITE,
        outlineColor: Cesium.Color.BLACK,
        outlineWidth: 1,
        style: Cesium.LabelStyle.FILL_AND_OUTLINE,
        scaleByDistance: new Cesium.NearFarScalar(1000, 1.0, 50000, 0.3)
      }
    })
    
    this.dynamicPaths.set(id, entity)
    
    // 飞行到起点
    const firstPos = Cesium.Cartesian3.fromDegrees(waypoints[0].lon, waypoints[0].lat, waypoints[0].alt + 1000)
    this.viewer.camera.flyTo({
      destination: firstPos,
      orientation: {
        heading: Cesium.Math.toRadians(0),
        pitch: Cesium.Math.toRadians(-30),
        roll: 0
      },
      duration: 2
    })
    
    return entity
  }
  
  /**
   * 创建车辆移动轨迹
   */
  createVehiclePath(
    id: string,
    routePoints: Array<{
      lon: number
      lat: number
      speed: number // km/h
    }>,
    options: {
      color?: Cesium.Color
    } = {}
  ): Entity {
    const {
      color = Cesium.Color.YELLOW
    } = options
    
    // 创建位置采样
    const position = new Cesium.SampledPositionProperty()
    
    let currentTime = 0
    for (let i = 0; i < routePoints.length; i++) {
      const wp = routePoints[i]
      
      // 计算到下一个点的时间
      let distance = 0
      if (i < routePoints.length - 1) {
        const next = routePoints[i + 1]
        distance = this.haversineDistance(wp.lat, wp.lon, next.lat, next.lon)
      }
      
      const travelTime = distance / wp.speed * 3.6 // 转换为秒
      
      const time = Cesium.JulianDate.addSeconds(
        Cesium.JulianDate.now(),
        currentTime,
        new Cesium.JulianDate()
      )
      
      const cartesian = Cesium.Cartesian3.fromDegrees(wp.lon, wp.lat, 0)
      position.addSample(time, cartesian)
      
      currentTime += travelTime
    }
    
    // 创建实体
    const entity = this.viewer.entities.add({
      id: id,
      name: '车辆',
      position: position,
      orientation: new Cesium.VelocityOrientationProperty(position),
      model: {
        uri: 'https://cesium.com/public/SandcastleSampleData/Models/GroundVehicle.glb',
        scale: 1.0
      },
      path: {
        resolution: 1,
        material: color.withAlpha(0.5),
        width: 5
      }
    })
    
    this.dynamicPaths.set(id, entity)
    
    return entity
  }
  
  /**
   * 创建动态圆（扩散动画）
   */
  createPulsingCircle(
    id: string,
    position: Cesium.Cartesian3,
    options: {
      radius?: number
      color?: Cesium.Color
      duration?: number
    } = {}
  ): Entity {
    const {
      radius = 1000,
      color = Cesium.Color.RED,
      duration = 2000
    } = options
    
    let scale = 0
    let growing = true
    
    const entity = this.viewer.entities.add({
      id: id,
      position: position,
      ellipse: {
        semiMajorAxis: new Cesium.CallbackProperty(() => {
          // 动态缩放
          if (growing) {
            scale += 2
            if (scale >= radius) growing = false
          } else {
            scale -= 2
            if (scale <= 0) growing = true
          }
          return scale
        }, false),
        semiMinorAxis: new Cesium.CallbackProperty(() => scale, false),
        material: new Cesium.ColorMaterialProperty(
          new Cesium.CallbackProperty(() => {
            const alpha = 1 - (scale / radius)
            return color.withAlpha(alpha * 0.5)
          }, false)
        ),
        height: 0,
        extrudedHeight: 0,
        outline: true,
        outlineColor: color
      }
    })
    
    this.entityAnimations.set(id, entity)
    
    return entity
  }
  
  /**
   * 创建雷达扫描效果
   */
  createRadarSweep(
    id: string,
    position: Cesium.Cartesian3,
    options: {
      radius?: number
      color?: Cesium.Color
      rotationSpeed?: number
    } = {}
  ): Entity {
    const {
      radius = 50000,
      color = Cesium.Color.fromBytes(0, 255, 0, 200),
      rotationSpeed = 0.02
    } = options
    
    let angle = 0
    
    const entity = this.viewer.entities.add({
      id: id,
      position: position,
      // 雷达扫描使用自定义材质
      rectangle: {
        coordinates: new Cesium.CallbackProperty(() => {
          // 计算扫描扇形
          return Cesium.Rectangle.fromDegrees(
            -radius / 111000,
            -radius / 111000,
            radius / 111000,
            radius / 111000
          )
        }, false),
        material: new Cesium.ColorMaterialProperty(
          new Cesium.CallbackProperty(() => {
            return color.withAlpha(0.3)
          }, false)
        ),
        height: 0,
        extrudedHeight: 100
      }
    })
    
    // 动态更新角度
    const updateInterval = setInterval(() => {
      angle += rotationSpeed
      if (angle > Math.PI * 2) angle = 0
    }, 50)
    
    // 存储interval引用以便清理
    (entity as any)._updateInterval = updateInterval
    
    this.entityAnimations.set(id, entity)
    
    return entity
  }
  
  /**
   * 创建3D箭头指示器
   */
  create3DArrow(
    id: string,
    startPosition: Cesium.Cartesian3,
    endPosition: Cesium.Cartesian3,
    options: {
      color?: Cesium.Color
      width?: number
      headLength?: number
      headWidth?: number
    } = {}
  ): Entity {
    const {
      color = Cesium.Color.ORANGE,
      width = 10,
      headLength = 50,
      headWidth = 25
    } = options
    
    // 计算方向
    const direction = Cesium.Cartesian3.subtract(
      endPosition,
      startPosition,
      new Cesium.Cartesian3()
    )
    Cesium.Cartesian3.normalize(direction, direction)
    
    // 创建锥体表示箭头
    const arrow = this.viewer.entities.add({
      id: id,
      position: startPosition,
      orientation: Cesium.Transforms.headingPitchRollQuaternion(
        startPosition,
        new Cesium.HeadingPitchRoll(
          Math.atan2(direction.x, direction.y),
          Math.asin(direction.z),
          0
        )
      ),
      cone: {
        length: headLength * 2,
        topRadius: 0,
        bottomRadius: headWidth,
        material: color.withAlpha(0.8),
        outline: true,
        outlineColor: color
      }
    })
    
    // 添加线段表示箭头杆
    const line = this.viewer.entities.add({
      id: `${id}_line`,
      polyline: {
        positions: [startPosition, endPosition],
        width: width,
        material: color
      }
    })
    
    this.entityAnimations.set(id, arrow)
    this.entityAnimations.set(`${id}_line`, line)
    
    return arrow
  }
  
  /**
   * 创建动态热力图
   */
  createHeatmap(
    id: string,
    points: Array<{
      lon: number
      lat: number
      intensity: number
    }>,
    options: {
      radius?: number
      minIntensity?: number
      maxIntensity?: number
    } = {}
  ): DataSource | null {
    const {
      radius = 20,
      minIntensity = 0,
      maxIntensity = 1
    } = options
    
    try {
      // 创建GeoJSON数据
      const geojson = {
        type: 'FeatureCollection',
        features: points.map(p => ({
          type: 'Feature',
          geometry: {
            type: 'Point',
            coordinates: [p.lon, p.lat]
          },
          properties: {
            intensity: p.intensity
          }
        }))
      }
      
      // 加载数据源
      const dataSource = new Cesium.GeoJsonDataSource(id)
      
      dataSource.load(geojson, {
        stroke: Cesium.Color.TRANSPARENT,
        fill: Cesium.Color.TRANSPARENT,
        markerSize: radius,
        markerColor: Cesium.Color.TRANSPARENT,
        strokeWidth: 0
      }).then(() => {
        this.viewer.dataSources.add(dataSource)
        
        // 应用颜色映射
        const entities = dataSource.entities.values
        entities.forEach((entity: any) => {
          const intensity = entity.properties?.intensity?.getValue() || 0
          const normalized = (intensity - minIntensity) / (maxIntensity - minIntensity)
          
          // 颜色从蓝到红
          entity.point = {
            pixelSize: radius * 2,
            color: new Cesium.Color(
              normalized,
              0,
              1 - normalized,
              0.6
            )
          }
        })
      })
      
      return dataSource
    } catch (e) {
      console.error('Failed to create heatmap:', e)
      return null
    }
  }
  
  /**
   * 创建流动线条
   */
  createFlowingLine(
    id: string,
    positions: Cesium.Cartesian3[],
    options: {
      color?: Cesium.Color
      speed?: number
      width?: number
      gapColor?: Cesium.Color
      dashLength?: number
    } = {}
  ): Entity {
    const {
      color = Cesium.Color.CYAN,
      speed = 1.0,
      width = 3,
      gapColor = Cesium.Color.TRANSPARENT,
      dashLength = 16.0
    } = options
    
    let offset = 0
    
    const entity = this.viewer.entities.add({
      id: id,
      polyline: {
        positions: positions,
        width: width,
        material: new Cesium.PolylineDashMaterialProperty({
          color: color,
          dashLength: dashLength,
          gapColor: gapColor,
          dashPattern: 0xFFFF,
          getAnimationTranslation: () => {
            offset += speed
            if (offset > dashLength * 2) offset = 0
            return new Cesium.Cartesian2(offset, 0)
          }
        })
      }
    })
    
    this.entityAnimations.set(id, entity)
    
    return entity
  }
  
  /**
   * 创建闪烁标记
   */
  createBlinkingMarker(
    id: string,
    position: Cesium.Cartesian3,
    options: {
      color?: Cesium.Color
      blinkInterval?: number
      minScale?: number
      maxScale?: number
    } = {}
  ): Entity {
    const {
      color = Cesium.Color.RED,
      blinkInterval = 500,
      minScale = 0.8,
      maxScale = 1.2
    } = options
    
    let scale = minScale
    let growing = true
    
    const entity = this.viewer.entities.add({
      id: id,
      position: position,
      billboard: {
        image: this.createCircleImage(color),
        scale: new Cesium.CallbackProperty(() => {
          // 闪烁动画
          if (growing) {
            scale += 0.02
            if (scale >= maxScale) growing = false
          } else {
            scale -= 0.02
            if (scale <= minScale) growing = true
          }
          return scale
        }, false),
        color: new Cesium.CallbackProperty(() => {
          const alpha = 0.5 + (scale - minScale) / (maxScale - minScale) * 0.5
          return color.withAlpha(alpha)
        }, false)
      }
    })
    
    this.entityAnimations.set(id, entity)
    
    return entity
  }
  
  /**
   * 创建扩散环
   */
  createRippleEffect(
    id: string,
    position: Cesium.Cartesian3,
    options: {
      maxRadius?: number
      color?: Cesium.Color
      duration?: number
      ringCount?: number
    } = {}
  ): Cesium.Entity[] {
    const {
      maxRadius = 5000,
      color = Cesium.Color.CYAN,
      duration = 3000,
      ringCount = 3
    } = options
    
    const rings: Cesium.Entity[] = []
    
    for (let i = 0; i < ringCount; i++) {
      const ring = this.viewer.entities.add({
        id: `${id}_${i}`,
        position: position,
        ellipse: {
          semiMajorAxis: new Cesium.CallbackProperty((time: Cesium.JulianDate) => {
            const elapsed = (time.secondsOfDay || 0) * 1000 % duration
            const progress = elapsed / duration
            const delay = i / ringCount
            const adjustedProgress = (progress + delay) % 1
            return adjustedProgress * maxRadius
          }, false) as any,
          semiMinorAxis: new Cesium.CallbackProperty((time: Cesium.JulianDate) => {
            const elapsed = (time.secondsOfDay || 0) * 1000 % duration
            const progress = elapsed / duration
            const delay = i / ringCount
            const adjustedProgress = (progress + delay) % 1
            return adjustedProgress * maxRadius
          }, false) as any,
          material: color.withAlpha(0.5),
          outline: true,
          outlineColor: color,
          outlineWidth: 2,
          height: 0,
          extrudedHeight: 0
        }
      })
      
      rings.push(ring)
      this.entityAnimations.set(`${id}_${i}`, ring)
    }
    
    return rings
  }
  
  /**
   * 移除实体
   */
  removeEntity(id: string) {
    const animation = this.animations.get(id)
    if (animation) {
      animation.stop()
      this.animations.delete(id)
    }
    
    const dynamicPath = this.dynamicPaths.get(id)
    if (dynamicPath) {
      this.viewer.entities.remove(dynamicPath)
      this.dynamicPaths.delete(id)
    }
    
    const entityAnim = this.entityAnimations.get(id)
    if (entityAnim) {
      this.viewer.entities.remove(entityAnim)
      
      // 清理interval
      const interval = (entityAnim as any)._updateInterval
      if (interval) clearInterval(interval)
      
      this.entityAnimations.delete(id)
    }
  }
  
  /**
   * 清空所有动画
   */
  clearAll() {
    // 停止所有动画
    this.animations.forEach(anim => anim.stop())
    this.animations.clear()
    
    // 移除所有实体
    this.dynamicPaths.forEach((entity, id) => {
      this.viewer.entities.remove(entity)
    })
    this.dynamicPaths.clear()
    
    this.entityAnimations.forEach((entity, id) => {
      const interval = (entity as any)._updateInterval
      if (interval) clearInterval(interval)
      this.viewer.entities.remove(entity)
    })
    this.entityAnimations.clear()
  }
  
  /**
   * 销毁管理器
   */
  destroy() {
    this.clearAll()
  }
  
  // ==================== 辅助方法 ====================
  
  private haversineDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371 // 地球半径(km)
    const dLat = this.toRad(lat2 - lat1)
    const dLon = this.toRad(lon2 - lon1)
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
            Math.cos(this.toRad(lat1)) * Math.cos(this.toRad(lat2)) *
            Math.sin(dLon/2) * Math.sin(dLon/2)
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
    return R * c
  }
  
  private toRad(deg: number): number {
    return deg * Math.PI / 180
  }
  
  private createCircleImage(color: Cesium.Color): HTMLCanvasElement {
    const canvas = document.createElement('canvas')
    canvas.width = 64
    canvas.height = 64
    const ctx = canvas.getContext('2d')!
    
    const gradient = ctx.createRadialGradient(32, 32, 0, 32, 32, 32)
    gradient.addColorStop(0, `rgba(${color.red * 255}, ${color.green * 255}, ${color.blue * 255}, 1)`)
    gradient.addColorStop(0.5, `rgba(${color.red * 255}, ${color.green * 255}, ${color.blue * 255}, 0.5)`)
    gradient.addColorStop(1, 'rgba(0, 0, 0, 0)')
    
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, 64, 64)
    
    return canvas
  }
}
