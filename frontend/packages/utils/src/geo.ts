export function degreesToRadians(degrees: number): number {
  return degrees * (Math.PI / 180)
}

export function radiansToDegrees(radians: number): number {
  return radians * (180 / Math.PI)
}

export function haversineDistance(
  lon1: number, lat1: number,
  lon2: number, lat2: number
): number {
  const R = 6371 // km
  const dLat = degreesToRadians(lat2 - lat1)
  const dLon = degreesToRadians(lon2 - lon1)
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(degreesToRadians(lat1)) * Math.cos(degreesToRadians(lat2)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

export function calculateBearing(
  lon1: number, lat1: number,
  lon2: number, lat2: number
): number {
  const dLon = degreesToRadians(lon2 - lon1)
  const lat1Rad = degreesToRadians(lat1)
  const lat2Rad = degreesToRadians(lat2)
  const y = Math.sin(dLon) * Math.cos(lat2Rad)
  const x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
    Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(dLon)
  return radiansToDegrees(Math.atan2(y, x))
}
