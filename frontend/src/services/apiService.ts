import axios, { AxiosInstance, AxiosError } from 'axios';
import { TransitResponse, VehiclePosition, Alert, RoutePlan } from '../types/transit.types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// Attach trace ID to every request
apiClient.interceptors.request.use((config) => {
  config.headers['X-Trace-Id'] = crypto.randomUUID();
  return config;
});

// Normalise error responses
apiClient.interceptors.response.use(
  (res) => res,
  (error: AxiosError) => {
    const message =
      (error.response?.data as { detail?: string })?.detail ??
      error.message ??
      'An unexpected error occurred';
    return Promise.reject(new Error(message));
  }
);

export const transitApi = {
  getTransitData: (city: string, route: string): Promise<TransitResponse> =>
    apiClient
      .get<TransitResponse>('/transit', { params: { city, route } })
      .then((r) => r.data),

  getVehicles: (city: string, routeId: string): Promise<VehiclePosition[]> =>
    apiClient
      .get<VehiclePosition[]>(`/transit/${routeId}/vehicles`, { params: { city } })
      .then((r) => r.data),

  getAlerts: (city: string, routeId: string): Promise<Alert[]> =>
    apiClient
      .get<Alert[]>(`/transit/${routeId}/alerts`, { params: { city } })
      .then((r) => r.data),

  planRoute: (city: string, from: string, to: string): Promise<RoutePlan> =>
    apiClient
      .get<RoutePlan>('/routes/plan', { params: { city, from, to } })
      .then((r) => r.data),

  getAlternatives: (city: string, routeId: string): Promise<RoutePlan> =>
    apiClient
      .get<RoutePlan>(`/routes/${routeId}/alternatives`, { params: { city } })
      .then((r) => r.data),

  checkHealth: (): Promise<{ status: string }> =>
    apiClient.get('/health').then((r) => r.data),
};
