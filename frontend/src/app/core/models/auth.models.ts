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
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}
