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
    stdio: ['ignore', 'pipe', 'pipe']
  });

  let backendOutput = '';
  const captureBackendOutput = (chunk) => {
    backendOutput = `${backendOutput}${chunk.toString()}`.slice(-8000);
  };

  // Pipe обязательно нужно читать: DEBUG + SQL-логи иначе блокируют Maven-процесс.
  backendProcess.stdout.on('data', captureBackendOutput);
  backendProcess.stderr.on('data', captureBackendOutput);

  let backendProcessError = null;
  backendProcess.on('error', (error) => {
    backendProcessError = error;
  });

  global.backendProcess = backendProcess;

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
    const output = backendOutput.trim();
    const reason = backendProcessError?.message || (output ? `Последние логи:\n${output}` : 'порт 8080 недоступен');
    backendProcess.kill('SIGTERM');
    throw new Error(`Тестовый бэкенд не запустился. ${reason}`);
  }

  console.log('✅ Глобальная настройка завершена');
}

export default globalSetup;
