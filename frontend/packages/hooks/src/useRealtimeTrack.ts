/**
 * useRealtimeTrack - Cesium 实时轨迹 WebSocket Hook
 *
 * 连接 WebSocket 实时订阅轨迹数据，在 Cesium 地图上实时渲染移动实体
 *
 * 使用方式：
 * const { subscribe, unsubscribe, connected, entities } = useRealtimeTrack(viewer)
 *
 * subscribe('trajectory.vehicle.001')
 */

import { ref, shallowRef, onUnmounted } from 'vue'
import type { Viewer } from 'cesium'

export interface RealtimeEntity {
  id: string
  position: [number, number] // [lon, lat]
  timestamp?: string
  heading?: number
  speed?: number
  [key: string]: any
}

export interface UseRealtimeTrackOptions {
  /** WebSocket 基础 URL，默认从环境变量获取 */
  wsUrl?: string
  /** 重连间隔（毫秒），默认 3000 */
  reconnectInterval?: number
  /** 最大重连次数，默认 10 */
  maxReconnects?: number
}

export function useRealtimeTrack(
  viewer: Viewer,
  options: UseRealtimeTrackOptions = {}
) {
  const {
    wsUrl = import.meta.env.VITE_API_URL?.replace(/^http/, 'ws') + '/ws/v1/realtime',
    reconnectInterval = 3000,
    maxReconnects = 10
  } = options

  const connected = ref(false)
  const reconnectCount = ref(0)
  const subscribedChannels = shallowRef<Set<string>>(new Set())
  const entities = shallowRef<Map<string, any>>(new Map())

  let socket: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  function connect() {
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      return
    }

    try {
      socket = new WebSocket(wsUrl)

      socket.onopen = () => {
        connected.value = true
        reconnectCount.value = 0
        console.debug('[RealtimeTrack] WebSocket 已连接')

        // 重新订阅之前的频道
        if (subscribedChannels.value.size > 0) {
          subscribe(Array.from(subscribedChannels.value))
        }
      }

      socket.onmessage = (event) => {
        try {
          const msg = JSON.parse(event.data)
          handleMessage(msg)
        } catch (e) {
          console.error('[RealtimeTrack] 解析消息失败:', e)
        }
      }

      socket.onclose = (event) => {
        connected.value = false
        console.debug('[RealtimeTrack] WebSocket 连接关闭:', event.code, event.reason)

        // 自动重连
        if (reconnectCount.value < maxReconnects) {
          reconnectCount.value++
          console.debug(`[RealtimeTrack] ${reconnectInterval}ms 后重连... (${reconnectCount.value}/${maxReconnects})`)
          reconnectTimer = setTimeout(connect, reconnectInterval)
        }
      }

      socket.onerror = (error) => {
        console.error('[RealtimeTrack] WebSocket 错误:', error)
      }
    } catch (e) {
      console.error('[RealtimeTrack] 连接失败:', e)
    }
  }

  function handleMessage(msg: any) {
    switch (msg.type) {
      case 'entity_update':
        updateCesiumEntity(msg.channel, msg.data)
        break
      case 'geo_event':
        updateCesiumEntity(msg.channel, msg.data)
        break
      case 'alert':
        handleAlert(msg.channel, msg.data)
        break
      case 'connected':
        console.debug('[RealtimeTrack] 收到连接确认:', msg)
        break
      case 'error':
        console.error('[RealtimeTrack] 服务器错误:', msg.message)
        break
      default:
        console.debug('[RealtimeTrack] 收到未知消息:', msg.type)
    }
  }

  function updateCesiumEntity(channel: string, data: RealtimeEntity) {
    const { id, position, heading, speed, ...rest } = data

    try {
      if (!position || position.length < 2) return

      const [lon, lat] = position

      let entity = viewer.entities.getById(id)
      if (!entity) {
        entity = viewer.entities.add({
          id,
          description: JSON.stringify({ channel, ...rest }),
        })
      }

      // 更新位置
      entity.position = Cesium.Cartesian3.fromDegrees(lon, lat)

      // 更新方向（如果提供）
      if (heading !== undefined) {
        entity.orientation = Cesium.Transforms.headingPitchRollQuaternion(
          Cesium.Cartesian3.fromDegrees(lon, lat),
          new Cesium.HeadingPitchRoll(Cesium.Math.toRadians(heading), 0, 0)
        )
      }

      // 更新属性
      entities.value.set(id, data)

      // 记录轨迹点（可选）
      if (viewer.dataSourceDisplay) {
        // Cesium 会自动更新渲染
      }
    } catch (e) {
      console.error('[RealtimeTrack] 更新 Cesium 实体失败:', e)
    }
  }

  function handleAlert(channel: string, data: any) {
    console.warn('[RealtimeTrack] 收到告警:', channel, data)
    // TODO: 触发告警 UI 提示
  }

  function send(data: object) {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(data))
    }
  }

  function subscribe(channels: string[]) {
    const newChannels = channels.filter(ch => !subscribedChannels.value.has(ch))
    subscribedChannels.value = new Set([...subscribedChannels.value, ...newChannels])

    send({
      type: 'subscribe',
      channels
    })
  }

  function unsubscribe(channels: string[]) {
    subscribedChannels.value = new Set(
      [...subscribedChannels.value].filter(ch => !channels.includes(ch))
    )

    send({
      type: 'unsubscribe',
      channels
    })
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (socket) {
      socket.close()
      socket = null
    }
    connected.value = false
    reconnectCount.value = 0
  }

  // 生命周期清理
  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    reconnectCount,
    subscribedChannels,
    entities,
    connect,
    disconnect,
    subscribe,
    unsubscribe
  }
}
