import { spawn } from 'node:child_process'

const commands = [
  ['demo', ['run', 'dev:demo-server']],
  ['dashboard', ['run', 'dev:dashboard']],
  ['totem', ['run', 'dev:totem']],
]

const colors = ['\x1b[35m', '\x1b[36m', '\x1b[33m']
const children = commands.map(([name, args], index) => {
  const child = spawn('npm', args, { shell: true, stdio: ['inherit', 'pipe', 'pipe'] })
  const print = (chunk) => process.stdout.write(`${colors[index]}[${name}]\x1b[0m ${chunk}`)
  child.stdout.on('data', print)
  child.stderr.on('data', print)
  return child
})

const stop = () => {
  children.forEach((child) => child.kill())
  process.exit()
}

process.on('SIGINT', stop)
process.on('SIGTERM', stop)
