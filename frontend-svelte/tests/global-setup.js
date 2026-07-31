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

  // Если бэкенд уже запущен (например, локально для отладки) — переиспользуем его
  if (await isBackendUp()) {
    console.log('♻️ Бэкенд уже запущен, переиспользуем существующий процесс');
    global.backendProcess = null;
    return;
  }

  // Запускаем бэкенд сервер для тестов
  const backendProcess = spawn('mvn', [
    'spring-boot:run',
    '-Dspring-boot.run.profiles=test',
    '-Dspring-boot.run.useTestClasspath=true',
    '-Dspring-boot.run.jvmArguments=-Xmx512m'
  ], {
    cwd: '../backend',
    stdio: 'pipe'
  });

  // Ждем запуска бэкенда (максимум 60 секунд, шаг 2 секунды)
  console.log('⏳ Ожидание запуска бэкенда...');
  let up = false;
  for (let i = 0; i < 30; i++) {
    await new Promise(resolve => setTimeout(resolve, 2000));
    if (await isBackendUp()) { up = true; break; }
    if (backendProcess.exitCode !== null) break;
  }

  if (up) {
    console.log('✅ Бэкенд успешно запущен');
  } else {
    console.log('❌ Бэкенд не отвечает за отведённое время');
  }

  // Сохраняем процесс для последующего использования
  global.backendProcess = backendProcess;

  console.log('✅ Глобальная настройка завершена');
}

export default globalSetup;
