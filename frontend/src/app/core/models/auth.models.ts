export interface User {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string;
  status: string;
  onLeave: boolean;
  leaveNote?: string;
  role: Role;
  permissions: string[]; // aggregated permissions e.g. ["PROJECT:CREATE", "TASK:APPROVE"]
}

export interface Role {
  id: number;
  name: string;
  description: string;
  isSystemRole: boolean;
  permissions?: Permission[];
}

export interface Permission {
  id: number;
  module: string;
  action: string;
  description?: string;
}

export interface AuthResponse {
  token: string;
  type: string;
  /** Seconds until the access token expires. */
  expiresIn: number;
  refreshToken: string;
  user: User;
}

export interface ElevationResponse {
  elevationToken: string;
  scope: string;
  expiresAt: string;
}

/** Structured error body returned by every API endpoint. */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path: string;
  details: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}
