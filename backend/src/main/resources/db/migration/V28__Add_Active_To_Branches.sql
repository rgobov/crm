-- Add active column to branches table
ALTER TABLE branches 
ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL;

-- Create index for better performance on active branches
CREATE INDEX idx_branches_active ON branches(active);

-- Update all existing branches to be active (in case default doesn't work)
UPDATE branches SET active = TRUE WHERE active IS NULL;
