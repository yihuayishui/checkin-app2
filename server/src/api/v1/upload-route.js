// 图片上传路由
const { Router } = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const uuid = require('uuid');
const config = require('../../config');
const auth = require('../../middleware/auth');
const { ApiError } = require('../../middleware/error-handler');
const { success } = require('../../utils/helper');

// 确保上传目录存在
if (!fs.existsSync(config.UPLOAD_DIR)) {
  fs.mkdirSync(config.UPLOAD_DIR, { recursive: true });
}

const storage = multer.diskStorage({
  destination: config.UPLOAD_DIR,
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname) || '.jpg';
    cb(null, `${uuid.v4()}${ext}`);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB
  fileFilter: (req, file, cb) => {
    const allowed = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];
    const ext = path.extname(file.originalname).toLowerCase();
    cb(null, allowed.includes(ext));
  },
});

const router = Router();

router.post('/image', auth, upload.single('image'), (req, res) => {
  if (!req.file) throw new ApiError(400, '请选择图片文件');

  // 拼接访问 URL
  const url = `/uploads/${req.file.filename}`;
  res.json(success({ url, filename: req.file.filename }));
});

module.exports = router;
