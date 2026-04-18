export interface Pedido {
  id: number;
  state: string;
  generador: string;
  address: string;
  latitude: number;
  longitude: number;
}
export interface Ruta {
  waypoints: Waypoint[];
}

export interface Waypoint {
  lat: number;
  lng: number;
}
