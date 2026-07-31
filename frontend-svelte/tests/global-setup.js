import { chromium } from 'playwright';
import { spawn } from 'child_process';

async function isBackendUp() {
  try {
    const res = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'admin@test.com', password: 'password' }),
      signal: AbortSignal.timeout(5000)
    });
    return res.status === 200;
  } catch {
    return false;
  }
}

async function globalSetup(config) {
  console.log('🚀 Запуск глобальной настройки тестов...');

  // По умолчанию всегда запускаем СВЕЖИЙ тестовый бэкенд (H2 in-memory), а не переиспользуем
  // случайный процесс на :8080 — чтобы тестовые данные не попали в «живую» БД.
  // Опт-ин переиспользование только по явному маркеру (для локальной отладки):
  if (process.env.E2E_REUSE_BACKEND === '1' && await isBackendUp()) {
    console.log('♻️ E2E_REUSE_BACKEND=1: переиспользуем уже запущенный тестовый бэкенд');
    global.backendProcess = null;
    return;
  }

  const backendProcess = spawn('mvn', [
    'spring-boot:run',
    '-Dspring-boot.run.profiles=test',
    '-Dspring-boot.run.useTestClasspath=true',
    '-Dspring-boot.run.jvmArguments=-Xmx512m'
  ], {
    cwd: '../backend',
    stdio: 'pipe'
  });

  // Ждем запуска бэкенда (максимум 90 секунд, шаг 2 секунды)
  console.log('⏳ Ожидание запуска тестового бэкенда...');
  let up = false;
  for (let i = 0; i < 45; i++) {
    await new Promise(resolve => setTimeout(resolve, 2000));
    if (await isBackendUp()) { up = true; break; }
    if (backendProcess.exitCode !== null) break;
  }

  if (up) {
    console.log('✅ Тестовый бэкенд успешно запущен');
  } else {
    console.log('❌ Тестовый бэкенд не отвечает за отведённое время (занят ли порт 8080?)');
  }

  // Сохраняем процесс для последующего использования
  global.backendProcess = backendProcess;

  console.log('✅ Глобальная настройка завершена');
}

export default globalSetup;
