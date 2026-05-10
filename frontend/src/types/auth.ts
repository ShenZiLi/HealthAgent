export interface LoginRequest {
  username: string;
  password: string;
}

export interface UserInfo {
  username: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: UserInfo;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface Result<T> {
  code: number;
  message: string;
  data: T;
}
