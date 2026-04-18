export interface CreateRoadmapRequest {
  zone: string;
  employee: string;
  collectDate: string; // ISO format: "2025-05-28T12:00:00"
  exitHour: string;    // Format: "12:00:00"
  selectedOrderIds: number[];
  vehicleId?: number;
}

export interface RoadmapDto {
  roadmapId: number;
  zone: string;
  employee: string;
  collectDate: string;
  exitHour: string;
  state: string;
  totalGenerators?: number;
  details?: RoadmapDetailDto[];
}

export interface RoadmapDetailDto {
  id: number;
  generatorId: number;
  generatorName: string;
  generatorAddress: string;
  latitude?: number;
  longitude?: number;
  generatorType?: string;
  sealNumber?: string;
}
export interface EmployeeRoadmapResponse {
  roadmapId: number;
  collectDate: string;
  zone: string;
  generators: GeneratorInRoadmap[];
}

export interface GeneratorInRoadmap {
  id: number;
  latitude: number;
  longitude: number;
  generatorId: number;
  name: string;
  address: string;
  status: 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO';
  totalBags: number;
  collectedBags: number;
  seals: SealInfo[];
  requestedBags: number;
  orderId: number;
}

export interface SealInfo {
  sealId: number;
  sealNumber: string;
  state: string;
  qrContent: string;
}

export interface ScanQRRequest {
  qrContent: string;
  employeeId: number;
  roadmapId: number;
}

export interface ScanQRResponse {
  success: boolean;
  message: string;
  sealNumber: string;
  generatorName: string;
  newCollectedCount: number;
}
export interface StockInfo {
  bagId: number;
  size: string;
  price: number;
  currentStock: number;
  lowStock: boolean;
  minStock: number;
}
// 🆕 AGREGAR estas interfaces a tu archivo de interfaces (Interfaces.ts o models/)

export interface RoadmapCloseRequest {
  roadmapId: number;
  returnHour: string;     // Formato: "HH:mm:ss"
  finalKm: number;
  observations?: string;  // Opcional
}

export interface RoadmapCloseResponse {
  success: boolean;
  message: string;
  roadmapId: number;
  sealsDelivered: number;
  kmTraveled?: number;
  totalGenerators: number;
  workDurationMinutes?: number;
  closeDateTime: string;

  // Métodos utilitarios (opcionales, se pueden implementar en el component)
  getFormattedWorkDuration?(): string;
  getFormattedKm?(): string;
}
